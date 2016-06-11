#!/usr/bin/perl

# team-hints.perl
#
# Super hacky script to guess at a component's contributors.
#
# Assumes a directory structure consistent with the mrconfig
# files defined in various SciJava dotfiles repositories,
# including fiji and fiji-historical cloned into ~/code/fiji.

use strict;


##### Variables #####

my $baseDir = "$ENV{'HOME'}/code";

my $project = `basename \$(pwd)`;
chomp $project;
print "Project = $project\n";

my $oldDir = "$baseDir/fiji/fiji";
my $ancientDir = "$baseDir/fiji/fiji-historical";
my $projectPaths = "src-plugins/$project src-plugins/${project}_";


##### Scan project for info #####

# look at git commits
my $committerCmd = 'git shortlog -nse';
my @recentCommitters = `$committerCmd`;
my @oldCommitters = `cd $oldDir && $committerCmd -- $projectPaths`;
my @ancientCommitters = `cd $ancientDir && $committerCmd -- $projectPaths`;
my @committers = (@recentCommitters, @oldCommitters, @ancientCommitters);

# look at @author javadoc annotations
my $authorCmd = "git grep -h '\@author'";
my $authorFilter = "sed 's/.*\@author //' | sort | uniq -c";
my @recentAuthors = `git grep -h '\@author' | $authorFilter`;
my @oldAuthors = `cd $oldDir && $authorCmd -- $projectPaths | $authorFilter`;
my @ancientAuthors = `cd $ancientDir && $authorCmd -- $projectPaths | $authorFilter`;
my @authors = (@recentAuthors, @oldAuthors, @ancientAuthors);

print "\n[Devs]\n";
print @committers;

print "\n[Authors]\n";
print @authors;

# infer the founder(s)
my $founderCmd = "git log --all --pretty='format:%cn <%ce>'";
my $founderFilter = "tail -n 1";
my @recentFounder = `echo "\$($founderCmd | $founderFilter)"`;
my @oldFounder = `echo "\$(cd $oldDir && $founderCmd -- $projectPaths | $founderFilter)"`;
my @ancientFounder = `echo "\$(cd $ancientDir && $founderCmd -- $projectPaths | $founderFilter)"`;
my @founders = (@recentFounder, @oldFounder, @ancientFounder);

print "\n[Possible Founders]\n";
print @founders;

my $maintainerCmd = "git shortlog -nse --since='60 days ago'";
my @maintainers = `$maintainerCmd`;

print "\n[Possible Maintainers]\n";
print @maintainers;

sub personName($) {
  my $q = shift;
  $q =~ s/\s*\d*\s*//;
  $q =~ s/\s*[<(].*//;
  return $q;
}

# Extract author names
print "\n[Author Names]\n";
for my $author (@authors) {
  chomp $author;
  my $name = personName($author);
  print "$name\n";
}
