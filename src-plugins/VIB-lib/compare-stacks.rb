#!/usr/bin/ruby -w

require 'getoptlong'

def usage
	print <<EOF
Usage: compare-stacks [OPTION] <fileA> <fileB>"

 -t <SUBSTRING>, --title-matches=<SUBSTRING>
                    Only use images whose titles match <SUBSTRING>
 -c --close-others
                    Close all other images that might be open, so
                    we're just left with the overlay
EOF
end
  
options = GetoptLong.new(
  [ "--title-matches", "-t", GetoptLong::REQUIRED_ARGUMENT ],
  [ "--close-others",  "-c", GetoptLong::NO_ARGUMENT ]
)

substring = ""
close_others = false

begin
	options.each do |opt,arg|
          case opt
          when "--title-matches"
                  substring = arg
          when "--close-others"
                  close_others = true
          end
        end
rescue
	puts "Bad command line opion: #{$!}\n"
	usage
	exit
end

def total_memory
	total = nil
	kernel_name = `uname`.chomp
	case kernel_name
	when 'Darwin'
		lines = `vm_stat`.split("\n")
		total = 0
		lines.each do |line|
			if line =~ /Pages (free|active|inactive|wired down):\s+(\d+)/
				total += Integer($2)
			end
		end
		total = (total * 4096) / (1024 * 1024)
	when 'Linux'
		lines = `free -m`
		if lines =~ /^Mem:\s+(\d+)/
			total = Integer($1)
		else
			raise "Couldn't parse total memory from: free -m"
		end	
	else
		total = 512
	end
	total
end
		
# It's a bit painful getting the escaping right for doing
# this from the shell, so this is a small helper program.

vib_directory=File.dirname(File.expand_path(__FILE__))

require 'getoptlong'
options = GetoptLong.new(
  [ "--help", "-h", GetoptLong::NO_ARGUMENT ],
  [ "--substring", "-s", GetoptLong::REQUIRED_ARGUMENT ]
)

program_name = "compare-stacks"
memory="512m"

def usage
  print <<EOUSAGE
Usage: #{program_name} [OPTION]...

  -h, --help           Display this message and exit.
  -s <SUBSTRING>, --substring=<SUBSTRING>
                       Only use the images with <SUBSTRING> in their
                       titles.
  -k --keep-sources    Keep source images
  -c --close-others    Close non-matching images
EOUSAGE
end

substring = ""

keep_sources = false
close_others = false

begin
  options.each do |opt, arg|
    case opt
    when "--help"
      usage
      exit
    when "--substring"
      substring = arg
    when "--keep-sources"
      keep_sources = true
    when "--close-others"
      close_others = true
    end
  end
rescue
  print "Bad command line option: " + $! + "\n"
  usage
  exit
end

unless ARGV.length == 2
	usage
	exit( -1 )
end

fileA=ARGV[0]
fileB=ARGV[1]

unless FileTest.exist? fileA
	puts "File '#{fileA}' does not exist."
	exit( -1 )
end

unless FileTest.exist? fileB
	puts "File '#{fileB}' does not exist."
	exit( -1 )
end
	
fileA=File.expand_path(fileA)
fileB=File.expand_path(fileB)

macro_options = "substring=#{substring}"
macro_options += ",keep" if keep_sources
macro_options += ",close" if close_others

Dir.chdir( vib_directory ) {

	result = system( "java", "-Xmx#{memory}", "-Dplugins.dir=.", "-jar", "../ImageJ/ij.jar", "-port0", fileA, fileB, "-eval", "run('Overlay Registered','#{macro_options}');" )
	unless result
		puts "Running ImageJ failed."
	end
}
