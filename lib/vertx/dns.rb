# Copyright 2013 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

require 'core/wrapped_handler'
require 'socket'

module Vertx

  MxRecord =  Struct.new(:priority, :name)
  SrvRecord =  Struct.new(:priority, :weight, :port, :name, :protocol, :service, :target)

  # An async DNS Client
  #
  # @author Norman Maurer
  class DnsClient
    def initialize(*servers)
      @j_dns = org.vertx.java.platform.impl.JRubyVerticleFactory.vertx.createDnsClient(
          servers.map { |value| java.net.InetSocketAddress.new(value.ip_address, value.ip_port) }.to_java(java.net.InetSocketAddress))
    end

    # Try to lookup the A (ipv4) or AAAA (ipv6) record for the given name. The first found will be used.
    # The handler will be notified once the lookup completes.
    # @param [Block] hndlr A block to be used as the handler
    def lookup(name, &hndlr)
      @j_dns.lookup(name, ARWrappedHandler.new(hndlr) { |addr| addr.getHostAddress()})
      self
    end

    # Try to lookup the A (ipv4) record for the given name. The first found will be used.
    # The handler will be notified once the lookup completes.
    # @param [Block] hndlr A block to be used as the handler
    def lookup_4(name, &hndlr)
      @j_dns.lookup4(name, ARWrappedHandler.new(hndlr) { |addr| addr.getHostAddress()})
      self
    end

    # Try to AAAA (ipv6) record for the given name. The first found will be used.
    # The handler will be notified once the lookup completes.
    # @param [Block] hndlr A block to be used as the handler
    def lookup_6(name, &hndlr)
      @j_dns.lookup6(name, ARWrappedHandler.new(hndlr) { |addr| addr.getHostAddress()})
      self
    end

    # Try to resolve all A records for the given name.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def resolve_a(name, &hndlr)
      @j_dns.resolveA(name, ARWrappedHandler.new(hndlr) { |j_list|
        j_list.map { |item|
          item.getHostAddress()
        }
      })
      self
    end

    # Try to resolve all AAAA records for the given name.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def resolve_aaaa(name, &hndlr)
      @j_dns.resolveAAAA(name, ARWrappedHandler.new(hndlr) { |j_list|
        j_list.map { |item|
          item.getHostAddress()
        }
      })
      self
    end

    # Try to resolve all CNAME records for the given name.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def resolve_cname(name, &hndlr)
      @j_dns.resolveCNAME(name, ARWrappedHandler.new(hndlr))
      self
    end

    # Try to resolve all MX records for the given name.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def resolve_mx(name, &hndlr)
      @j_dns.resolveMX(name, ARWrappedHandler.new(hndlr) { |j_list|
        j_list.map { |item|
          MxRecord.new(item.priority(), item.name())
        }
      })
      self
    end

    # Try to resolve the PTR record for the given name.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def resolve_ptr(name, &hndlr)
      @j_dns.resolvePTR(name, ARWrappedHandler.new(hndlr))
      self
    end

    # Try to resolve all NS records for the given name.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def resolve_ns(name, &hndlr)
      @j_dns.resolveNS(name, ARWrappedHandler.new(hndlr))
      self
    end

    def resolve_txt(name, &hndlr)
      @j_dns.resolveTXT(name, ARWrappedHandler.new(hndlr))
      self
    end

    # Try to resolve all SRV records for the given name.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def resolve_srv(name, &hndlr)
      @j_dns.resolveSRV(name, ARWrappedHandler.new(hndlr) { |j_list|
        j_list.map { |item|
          SrvRecord.new(item.priority(), item.weight(), item.port(), item.name(), item.protocol(), item.service(), item.target())
        }
      })
      self
    end

    # Try to do a reverse lookup of an ipaddress. This is basically the same as doing trying to resolve a PTR record
    # but allows you to just pass in the ipaddress and not a valid ptr query string.
    # The handler will be notified once the operation completes.
    # @param [Block] hndlr A block to be used as the handler
    def reverse_lookup(name, &hndlr)
      @j_dns.reverseLookup(name, ARWrappedHandler.new(hndlr) { |addr| addr.getHostName()})
      self
    end

  end
end