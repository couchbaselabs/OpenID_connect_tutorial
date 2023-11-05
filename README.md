# OpenID\_connect\_tutorial

This repo is used by the Couchbase [OpenID Connect Tutorial](https://developer.couchbase.com/tutorial-syncgateway-openid-auth).

The goal of this tutorial is to:
- help setting quickly the different components (CB Server, Sync Gateway, CB-Lite (Java) code and the third party Identity Provider)
- establish an [Implicit flow authentication](https://blog.couchbase.com/oidc-implicit-flow-client-authentication-couchbase-sync-gateway/) using the OpenID Connect protocol.
- establish an [Authorization Code flow authentication](https://blog.couchbase.com/oidc-authorization-code-flow-client-authentication-couchbase-sync-gateway/) using the OpenID Connect protocol.

The source code of this tutorial is mainly derived from the Java CB-Lite "[Getting Started App](https://github.com/couchbase/docs-couchbase-lite/blob/c46306ac49587451adfd6b763036c5d25df51a72/modules/java/examples/GetStartedDesktop/src/main/java/com/couchbase/gettingstarted/Main.java)"
project.  

See complete tutorial at https://developer.couchbase.com/tutorial-syncgateway-openid-auth

<b>Disclaimer</b>: the source code has been updated on August 04 2021 to handle both implicit and authorization code flows.
That being said, the tutorial is, for now, only focusing on the Implicit flow authentication.

