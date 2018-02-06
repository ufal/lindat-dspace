/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;

import cz.cuni.mff.ufal.DSpaceApi;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;


public class ProcessBitstreams extends AbstractCurationTask implements Consumer {

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(ProcessBitstreams.class);

    public static String schema = "local";
    public static String element = "bitstream";
    public static String qualifier = "file";

    private int status = Curator.CURATE_UNSET;

    public static int ERROR = -1;
    public static int SKIPPED = 1;
    public static int OK = 0;

    // curator
    //

	@Override
	public int perform(DSpaceObject dso) throws IOException {

        // Unless this is  an item, we'll skip this item
        status = Curator.CURATE_SKIP;
        StringBuilder results = new StringBuilder();

        if (dso instanceof Item) {
            try {
                Item item = (Item)dso;
                boolean processed = processItem(item);
                if ( processed ) {
                    status = Curator.CURATE_SUCCESS;
                }
            } catch (Exception ex) {
                status = Curator.CURATE_FAIL;
                results.append(ex.getLocalizedMessage()).append("\n");
            }
        }
        
        report(results.toString());
        setResult(results.toString());
		return status;
	}

	boolean processItem(Item item) throws SQLException, AuthorizeException {
        int processed = 0;
        for ( Bundle bundle : item.getBundles("ORIGINAL") ) {
            for ( Bitstream b : bundle.getBitstreams() ) {
                if (OK == processBitstream(b)) {
                    processed += 1;
                }else if (SKIPPED == processBitstream(b)) {
                    processed += 1;
                }else {
                    processed = (0 < processed) ? -processed : processed;
                    processed -= 1;
                }
            }
        }
        return processed > 0;
	}

    // event consumer
    //
    public void initialize() throws Exception {
    }

    public void end(Context ctx) throws Exception {
    }

    public void finish(Context ctx) throws Exception {
    }

    public void consume(Context ctx, Event event) throws Exception {
        if (Constants.BITSTREAM != event.getSubjectType()) {
            return;
        }

        DSpaceObject subject = event.getSubject(ctx);
        DSpaceObject object = event.getObject(ctx);
        int et = event.getEventType();
        Bitstream b = (Bitstream)subject;

        if (null != subject) {
            if (Event.ADD == et || Event.CREATE == et) {
                processBitstream(b);
            } else if (Event.DELETE == et || Event.REMOVE == et) {
                // automatically removed
            }
        }

    }


    // do the processing
    //

    static int processBitstream(Bitstream b) throws SQLException, AuthorizeException {
        int ret;
        ret = addBitstreamContent(b);
        return ret;
    }

    static InputStream getIS(String mime, InputStream is) {

	    is = new BufferedInputStream(is);
	    InputStream ret = null;

	    switch (mime){
            case "application/x-gzip":
            case "application/gzip":
            case "application/x-xz":
                try{
                    CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(is);
                    ret = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(cis));
                }catch (CompressorException e){
                    log.error("Failed to extract known mime-type " + mime);
                    log.error(e);
                }catch (ArchiveException e){
                    log.debug("Not a compressed archive (eg. .tgz)");
                }
                break;
            case "application/zip":
            case "application/x-tar":
                try{
                    ret = new ArchiveStreamFactory().createArchiveInputStream(is);
                }catch (ArchiveException e){
                    log.error("Failed to extract known archive mime-type=" + mime);
                    log.error(e);
                }
                break;
            default: break;
        }

        if (ret == null && mime.startsWith("text/plain") ) {
        	ret = is;
        }
        return ret;
    }

    static int addBitstreamContent(Bitstream b) throws SQLException, AuthorizeException {
        Context context = new Context(Context.READ_ONLY);
        context.setCurrentUser(null);
        try {
            DSpaceApi.authorizeBitstream(context, b);
        }catch (AuthorizeException e){
            //Anonymous user not authorized don't generate preview
            context.complete();
            return SKIPPED;
        }finally {
            context.complete();
        }

        b.clearMetadata(schema, element, qualifier, Item.ANY);

        //
        try {
            String mime = b.getFormat().getMIMEType();
            InputStream is = getIS(mime, b.retrieve());
            if ( null == is ) {
                return SKIPPED;
            }
            if(is instanceof ArchiveInputStream) {
            	ArchiveInputStream ais = (ArchiveInputStream)is;
	            ArchiveEntry entry;
	            while ((entry = ais.getNextEntry()) != null) {
	                String content = String.format(
	                    "%s|%d", entry.getName(), entry.getSize()
	                );
	                b.addMetadata( schema, element, qualifier, Item.ANY, content );
	            }
            } else {
            	InputStreamReader r = new InputStreamReader(is);
            	char cbuf[] = new char[1000];
            	r.read(cbuf, 0, 1000);
            	b.addMetadata( schema, element, qualifier, Item.ANY, new String(cbuf) );
            }
        } catch (Exception e) {
            log.error(e);
            return ERROR;
        }

        b.update();
        return OK;
    }

}
