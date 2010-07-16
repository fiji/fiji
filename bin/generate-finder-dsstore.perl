#!/usr/bin/perl

$package = 'Mac-Finder-DSStore-0.94';
$url = 'http://ftp.cw.net/pub/CPAN/authors/id/W/WI/WIML/';

if (! -d $package) {
	`curl $url$package.tar.gz | tar xzvf -; (echo "Hello\n"; set -x; cd $package; perl Build.PL; ./Build)` ||
	die "Could not download/install $package";
}

$pwd = `pwd`;
chomp $pwd;
print "use lib ('$pwd/Mac-Finder-DSStore-0.94/blib/lib'); use Mac::Finder::DSStore qw(writeDSDBEntries makeEntries);\n";
eval "use lib ('$pwd/Mac-Finder-DSStore-0.94/blib/lib'); use Mac::Finder::DSStore qw(writeDSDBEntries makeEntries);";
use Mac::Memory;
use Mac::Files qw(NewAliasMinimal);

my $w = 472;
my $h = 354;
my $xOffset = 59;
my $arrowThickness = 14;

my $top = 100;
my $left = 200;
my $sidebar = 0;

my $arrowLength = int($arrowThickness * 2.5);
my $x1 = int(($xOffset + ($w - $arrowLength) / 2) / 2);
my $x2 = int($w - $x1);
my $y1 = int($h / 2);

chdir '/Volumes/Fiji';

&writeDSDBEntries(".DS_Store",
    &makeEntries(".",
        BKGD_alias => &NewAliasMinimal("/Volumes/Fiji/.background.jpg"),
        ICVO => 1,
        fwi0_flds => [ $top, $left, $top + $h, $left + $w + $sidebar, "icnv", 0, 0 ],
        fwsw => $sidebar,
	fwvh => $h,
	icgo => "\0\0\0\4\0\0\0\4",
        icvo => pack('A4 n A4 A4 n*', "icv4", 128, "none", "botm", 0, 0, 4, 0, 4, 1, 0, 6, 1),
        icvt => 1,
    ),
    &makeEntries("Fiji.app", Iloc_xy => [ $x1, $y1 ]),
    &makeEntries("Applications", Iloc_xy => [ $x2, $y1 ])
);
