# OpenID\_connect\_tutorial

The goal of this tutorial is to help setting quickly the different components (CB Server, Sync Gateway, CB-Lite code and the third party Identity Provider) and establish an implicit flow authentication using the OpenID Connect protocol.

The source code of this tutorial is mainly derived from the [Getting Started App](https://docs.couchbase.com/couchbase-lite/2.7/java-platform.html#building-a-getting-started-app)

## OpenID connection workflow

The tutorial will achieve to deploy and make collaborate all those differents components :

![](./client-auth.png)



Choice of the Identity Provider : KeyCloak is chosen (because it is well-known, Red hat supported, and of course because I like it 😄)

