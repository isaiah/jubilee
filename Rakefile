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
  Bundler.setup(:default, :development, :test)
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
  gem.homepage = "http://isaiah.github.io/jubilee"
  gem.license = "MIT"
  gem.summary = %Q{More than a server for rack applications.}
  gem.description = %Q{Jubilee is a rack server for JRuby built upon the high performance Vertx platform. It provides the best features of Vertx such as EventBus, SharedData, and clustering.}
  gem.email = "issaria@gmail.com"
  gem.authors = ["Isaiah Peng"]
  gem.version = Jubilee::Version::STRING
  gem.platform = "java"
  gem.files.include "lib/jubilee/jubilee.jar"
  # dependencies defined in Gemfile
end
Jeweler::RubygemsDotOrgTasks.new

require 'rake/testtask'
Rake::TestTask.new(:test) do |test|
  test.libs << 'lib' << 'test' << 'spec'
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
  version = Jubilee::Version::STRING

  rdoc.rdoc_dir = 'rdoc'
  rdoc.title = "jubilee #{version}"
  rdoc.rdoc_files.include('README*')
  rdoc.rdoc_files.include('lib/**/*.rb')
end

require 'ant'

DEST_PATH = "pkg/classes"
RESOURCE_PATH = "java/resources"

directory DEST_PATH

desc "Clean up build artifacts"
task :clean do
  rm_rf "pkg/classes"
  rm_rf "lib/jubilee/*.jar"
end

BUILDTIME_LIB_DIR = File.join(File.dirname(__FILE__), "jars")

desc "Compile the extension, need jdk7 because vertx relies on it"
task :compile => [DEST_PATH, "#{DEST_PATH}/META-INF"] do |t|
  ant.javac :srcdir => "java", :destdir => t.prerequisites.first,
    :source => "1.7", :target => "1.7", :debug => true, :includeantruntime => false,
    :classpath => "${java.class.path}:${sun.boot.class.path}:jars/vertx-core-2.1M3.jar:jars/netty-all-4.0.14.Final.jar:jars/jackson-core-2.2.2.jar:jars/jackson-databind-2.2.2.jar:jars/jackson-annotations-2.2.2.jar:jars/hazelcast-2.6.3.jar:jars/vertx-platform-2.1M3.jar"
end

desc "Copy META-INF"
task "#{DEST_PATH}/META-INF" => ["#{RESOURCE_PATH}/META-INF", "#{RESOURCE_PATH}/default-cluster.xml"] do |t|
  FileUtils.cp_r t.prerequisites.first, t.name, verbose: true
  cp t.prerequisites[1], DEST_PATH, verbose: true
end

desc "Build the jar"
task :jar => [:clean, :compile] do
  ant.jar :basedir => "pkg/classes", :destfile => "lib/jubilee/jubilee.jar"
end

task :build => :jar

require 'rspec/core/rake_task'
desc "Run integration tests"
RSpec::Core::RakeTask.new do |t|
  t.ruby_opts = ['-I"spec:lib"']
  t.pattern = 'spec/**/*_spec.rb'
end

task :spec => :jar
