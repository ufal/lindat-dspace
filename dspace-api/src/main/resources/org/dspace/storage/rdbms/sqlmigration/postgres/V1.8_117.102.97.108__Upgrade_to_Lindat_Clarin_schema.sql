--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

ALTER TABLE eperson ALTER COLUMN netid TYPE varchar(256);
ALTER TABLE eperson ADD welcome_info varchar(30);
ALTER TABLE eperson ADD last_login varchar(30);
ALTER TABLE eperson ADD can_edit_submission_metadata BOOL;

ALTER TABLE metadatafieldregistry ALTER COLUMN element TYPE VARCHAR(128);

ALTER TABLE handle ADD url varchar(2048);
