# encoding: utf-8

$:.unshift("./lib")
require 'rubygems'
require 'bundler'
require 'jubilee/version'

include\
  begin
    RbConfig
  rescue NameError
    Config
  end

begin
  Bundler.setup(:default, :development)
rescue Bundler::BundlerError => e
  $stderr.puts e.message
  $stderr.puts "Run `bundle install` to install missing gems"
  exit e.status_code
end
require 'rake'

require 'jeweler'
Jeweler::Tasks.new do |gem|
  # gem is a Gem::Specification... see http://docs.rubygems.org/read/chapter/20 for more options
  gem.name = "jubilee"
  gem.homepage = "http://github.com/isaiah/jubilee"
  gem.license = "MIT"
  gem.summary = %Q{More than a server for rack applications.}
  gem.description = %Q{Jubilee is a experimental webserver built for speed, it's based on Vertx.}
  gem.email = "issaria@gmail.com"
  gem.authors = ["Isaiah Peng"]
  # dependencies defined in Gemfile
end
Jeweler::RubygemsDotOrgTasks.new

require 'rake/testtask'
Rake::TestTask.new(:test) do |test|
  test.libs << 'lib' << 'test'
  test.pattern = 'test/**/test_*.rb'
  test.verbose = true
end

task :test => :jar

#require 'rcov/rcovtask'
#Rcov::RcovTask.new do |test|
#  test.libs << 'test'
#  test.pattern = 'test/**/test_*.rb'
#  test.verbose = true
#  test.rcov_opts << '--exclude "gems/*"'
#end

task :default => :test

require 'rdoc/task'
Rake::RDocTask.new do |rdoc|
  version = Jubilee::VERSION

  rdoc.rdoc_dir = 'rdoc'
  rdoc.title = "jubilee #{version}"
  rdoc.rdoc_files.include('README*')
  rdoc.rdoc_files.include('lib/**/*.rb')
end

require 'ant'

directory "pkg/classes"

desc "Clean up build artifacts"
task :clean do
  rm_rf "pkg/classes"
  rm_rf "lib/jubilee/*.jar"
end

BUILDTIME_LIB_DIR = File.join(File.dirname(__FILE__), "jars")

desc "Compile the extension, need jdk7 because vertx relies on it"
task :compile => "pkg/classes" do |t|
  ant.javac :srcdir => "java", :destdir => t.prerequisites.first,
    :source => "1.7", :target => "1.7", :debug => true,
    :classpath => "${java.class.path}:${sun.boot.class.path}:jars/vertx-core-2.1M3-SNAPSHOT.jar:jars/netty-all-4.0.13.Final.jar:jars/jackson-core-2.2.2.jar:jars/jackson-databind-2.2.2.jar:jars/jackson-annotations-2.2.2.jar:jars/hazelcast-2.6.3.jar"
end

desc "Build the jar"
task :jar => [:clean, :compile] do
  ant.jar :basedir => "pkg/classes", :destfile => "lib/jubilee/jubilee.jar", :includes => "**/*.class"
end
 
task :package => :jar

desc "Run the specs"
task :spec => :jar do
  ruby "-S", "spec", "spec"
end
