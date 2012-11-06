#!/usr/bin/perl

$pwd = $0;
$pwd =~ s/[^\/]*$//;
if ($pwd eq $0) {
	$pwd = `pwd`;
} else {
	$pwd = `cd "$pwd" && pwd`;
}
chomp $pwd;

$cpan_url = 'http://search.cpan.org/CPAN/authors/id/';
$cpan_mirror_url = 'http://mirrors.ibiblio.org/CPAN/authors/id/';
$perl5lib = $ENV{'PERL5LIB'};
foreach my $full_package (
		'M/MS/MSCHWERN/Test-Simple-0.98',
		'F/FD/FDALY/Test-Tester-0.108',
		'A/AD/ADAMK/Test-NoWarnings-1.04',
		'C/CN/CNANDOR/Mac-Carbon-0.82',
		'W/WI/WIML/Mac-Finder-DSStore-0.96'
	) {
	$package = $full_package;
	$package =~ s/.*\///;

	if (! -d "$pwd/$package") {
		#$url = `curl -i $cpan_url$full_package.tar.gz | sed -n 's|^Location: ||p'`;
		#chomp $url;
		$url = "$cpan_mirror_url$full_package.tar.gz";
		print "Downloading $url\n";
		`cd $pwd && curl $url | tar xzvf - && PERL5LIB=$perl5lib && export PERL5LIB && (cd $package && if test -f Build.PL; then perl Build.PL && ./Build; else perl Makefile.PL && make; fi)` ||
		die "Could not download/install $package";
	}

	print "use lib ('$pwd/$package/blib/lib');\n";
	eval "use lib ('$pwd/$package/blib/lib');";
	print("use lib ('$pwd/$package/blib/arch');");
	eval "use lib ('$pwd/$package/blib/arch');";
	$perl5lib .= ':' unless ($perl5lib eq '');
	$perl5lib .= "$pwd/$package/blib/lib:$pwd/$package/blib/arch";
}

eval "use Mac::Finder::DSStore qw(writeDSDBEntries makeEntries);";
eval "use Mac::Memory;";
eval "use Mac::Files qw(NewAliasMinimal);";

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

chdir $ARGV[0] || die "Could not chdir into $ARGV[0]";

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
