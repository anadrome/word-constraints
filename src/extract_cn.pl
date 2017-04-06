#!/usr/bin/perl

# Perl script to extract relations from ConceptNet that only involve a single
# word, since the longer ones are too weirdly phrased and impossible to guess
# to be useful. Saves RAM and increases loading speed substantially also.
# Takes input on stdin and writes to stdout.
# (Was used to generate conceptnet_singlewords.txt.)
# mjn 2008

while(<>)
{
   my $line = $_;
   $line =~ /^\(\S+ "(.*)" "(.*)" ".*"\)/;
   my $source = $1;
   my $target = $2;

   unless ($source =~ /\s/ || $target =~ /\s/)
   {
      print $line;
   }
}
