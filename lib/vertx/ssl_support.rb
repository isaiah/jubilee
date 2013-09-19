# Copyright 2011 the original author or authors.
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


module Vertx

  # A mixin module allowing SSL attributes to be set on classes
  #
  # @author {http://tfox.org Tim Fox}
  module SSLSupport

    # Set whether the server or client will use SSL.
    # @param [Boolean] val. If true then ssl will be used.
    # @return [] self. So multiple invocations can be chained.
    def ssl=(val)
      @j_del.setSSL(val)
      self
    end

    # Set or get ssl for fluent API
    def ssl(ssl = nil)
      if ssl
        @j_del.setSSL(ssl)
        self
      else
        @j_del.isSSL
      end
    end

    # Set the path to the SSL key store. This method should only be used with the client/server in SSL mode, i.e. after {#ssl=}
    # has been set to true.
    # The SSL key store is a standard Java Key Store, and should contain the client/server certificate. For a client, it's only necessary to supply
    # a client key store if the server requires client authentication via client certificates.
    # @param [String] val. The path to the key store
    # @return [] self. So multiple invocations can be chained.
    def key_store_path=(val)
      @j_del.setKeyStorePath(val)
      self
    end

    # Set or get key store path for fluent API
    def key_store_path(path = nil)
      if path
        @j_del.setKeyStorePath(path)
        self
      else
        @j_del.getKeyStorePath
      end
    end

    # Set the password for the SSL key store. This method should only be used with the client in SSL mode, i.e. after {#ssl=}
    # has been set to true.
    # @param [String] val. The password.
    # @return [] self. So multiple invocations can be chained.
    def key_store_password=(val)
      @j_del.setKeyStorePassword(val)
      self
    end

    # Set or get key store password for fluent API
    def key_store_password(password = nil)
      if password
        @j_del.setKeyStorePassword(password)
        self
      else
        @j_del.getKeyStorePassword
      end
    end

    # Set the path to the SSL trust store. This method should only be used with the client/server in SSL mode, i.e. after {#ssl=}
    # has been set to true.
    # The SSL trust store is a standard Java Key Store, and should contain the certificate(s) of the clients/servers that the server/client trusts. The SSL
    # handshake will fail if the server provides a certificate that the client does not trust, or if client authentication is used,
    # if the client provides a certificate the server does not trust.<p>
    # @param [String] val. The path to the trust store
    # @return [] self. So multiple invocations can be chained.
    def trust_store_path=(val)
      @j_del.setTrustStorePath(val)
      self
    end

    # Set or get trust store path for fluent API
    def trust_store_path(path = nil)
      if path
        @j_del.setTrustStorePath(path)
        self
      else
        @j_del.getTrustStorePath
      end
    end

    # Set the password for the SSL trust store. This method should only be used with the client in SSL mode, i.e. after {#ssl=}
    # has been set to true.
    # @param [String] val. The password.
    # @return [] self. So multiple invocations can be chained.
    def trust_store_password=(val)
      @j_del.setTrustStorePassword(val)
      self
    end

    # Set or get trust store password for fluent API
    def trust_store_password(password = nil)
      if password
        @j_del.setTrustStorePassword(password)
        self
      else
        @j_del.getTrustStorePassword
      end
    end

  end

  # A mixin module allowing Server SSL attributes to be set on classes
  #
  # @author {http://tfox.org Tim Fox}
  module ServerSSLSupport

    def client_auth_required=(val)
      @j_del.setClientAuthRequired(val)
      self
    end

    def client_auth_required(val = nil)
      if val
        @j_del.setClientAuthRequired(val)
        self
      else
        @j_del.isClientAuthRequired
      end
    end
  end

  # A mixin module allowing Client SSL attributes to be set on classes
  #
  # @author {http://tfox.org Tim Fox}
  module ClientSSLSupport

    def trust_all=(val)
      @j_del.setTrustAll(val)
      self
    end

    def trust_all(val = nil)
      if val
        @j_del.setTrustAll(val)
        self
      else
        @j_del.isTrustAll
      end
    end
  end
end