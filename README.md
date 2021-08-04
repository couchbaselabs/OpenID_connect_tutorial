# OpenID\_connect\_tutorial

This repo is used by the Couchbase [OpenID Connect Tutorial](https://docs.couchbase.com/tutorials/openid-connect-implicit-flow/index.html).

The goal of this tutorial is to:
- help setting quickly the different components (CB Server, Sync Gateway, CB-Lite (Java) code and the third party Identity Provider)
- establish an [Implicit flow authentication](https://blog.couchbase.com/oidc-implicit-flow-client-authentication-couchbase-sync-gateway/) using the OpenID Connect protocol.
- establish an [Authorization Code flow authentication](https://blog.couchbase.com/oidc-authorization-code-flow-client-authentication-couchbase-sync-gateway/) using the OpenID Connect protocol.

The source code of this tutorial is mainly derived from the Java CB-Lite "[Getting Started App](https://docs.couchbase.com/couchbase-lite/2.7/java-platform.html#building-a-getting-started-app)"
project.  

See complete tutorial at https://docs.couchbase.com/tutorials/openid-connect-implicit-flow/index.html

<b>Disclaimer</b>: the source code has been updated on August 04 2021 to handle both implicit and authorization code flows.
That being said, the tutorial is, for now, only focusing on the Implicit flow authentication.


