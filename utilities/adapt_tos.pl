#!/usr/bin/perl

use strict;
use warnings;
use Tie::File;

# define paths for TOS and local.properties
my $tosfile = '/opt/repository/sources/dspace/dspace-xmlui/src/main/webapp/themes/UFAL/lib/html/terms-of-service.html';
my $locpropfile = '/opt/repository/sources/dspace/local.properties';

my ($repo_name,$centre_legal_entity,$full_address,$country,$city);

# process local.properties to get the needed values
open (my $lp, '<', $locpropfile);

while (my $in = <$lp>)
{
	if ($in =~ /^dspace\.name/)
	{
		(undef,$repo_name) = split /=/, $in;
		chomp $repo_name;
		$repo_name =~ s/^\s//;
	}
	elsif ($in =~ /^lr.description.institution /)
	{
		(undef,$centre_legal_entity) = split /=/, $in;
		chomp $centre_legal_entity;
		$centre_legal_entity =~ s/^\s//;
	}
	elsif ($in =~ /^lr.description.location/)
	{
		(undef,$full_address) = split /=/, $in;
		chomp $full_address;
		$full_address =~ s/^\s//;
	}
	elsif ($in =~ /^lr.description.shortLocation/)
	{
		(undef,my $location) = split /=/, $in;
		chomp $location;
		($city,$country) = split /,/,  $location;
		$city =~ s/^\s//;
		$country =~ s/^\s//;
	}
}

# process TOS file to replace variables with values from local.properties
tie my @tosarray, 'Tie::File', $tosfile or die $!;
foreach my $line (@tosarray)
{
	if ($line =~ /REPOSITORY_NAME/)
	{
		$line =~ s/\$REPOSITORY_NAME/$repo_name/;
	}
	if ($line =~ /A_CENTRE_OR_UNIT_WITH_LEGAL_PERSONALITY/)
	{
		$line =~ s/\$A_CENTRE_OR_UNIT_WITH_LEGAL_PERSONALITY/$centre_legal_entity/;
	}
	if ($line =~ /FULL_ADDRESS/)
	{
		$line =~ s/\$FULL_ADDRESS/$full_address/;
	}
	if ($line =~ /COUNTRY/)
	{
		$line =~ s/\$COUNTRY/$country/;
	}
	if ($line =~ /CITY/)
	{
		$line =~ s/\$CITY/$city/;
	}
}

