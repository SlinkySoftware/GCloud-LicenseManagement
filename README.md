# Genesys Cloud License Management #

This tool allows you to manage concurrent licensing within Genesys Cloud.

It works by requiring an agent to check a license in and out, which then modifies their account within Genesys Cloud to allow or prevent login.

Agent authentication is provided using an external SAML account - OKTA, Azure AD and ADFS are supported. It will not authenticate the agent to Genesys Cloud as their account will be disabled.

The application uses a service account within Genesys Cloud with the appropriate permission to modify the users.
