package cz.cuni.mff.ufal.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class BibtexUtilTest {
    @Test
    public void bibtexifyUpper() {
        assertEquals("{\\'U}", BibtexUtil.bibtexify("Ú"));
    }

    @Test
    public void bibtexifySharpS() {
        assertEquals("{\\ss}", BibtexUtil.bibtexify("ß"));
    }

}