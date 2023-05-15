# Genesys Cloud License Management #

This tool allows you to manage concurrent licensing within Genesys Cloud. It works in conjuection with the Slinky Software Genesys Cloud 
provisioning tool, which operates as the administrative application. 
The License Management applicaiton is the front-end where users can check-out and check-in licenses.

It is required that each Genesys Cloud instance be configured for user login using Azure AD authentication. A dedicated Azure AD group 
per Genesys Cloud instance needs to be control access to the respective Enterprise Application.

When an user requests a license, checks are performed to ensure the user is configured into a particular license group. If there are 
enough licenses remaining, then they are allocated a license. This adds them to to the controlling Azure AD group. 

Each license is allocated for a particular time, after which it is expired. At expiry (or the user manually returning their license) 
the user is removed from the Azure AD group, thereby preventing login to Genesys Cloud, and are forcefully logged out by deleting
their active tokens within the respective Genesys Cloud instance.

Agent authentication to the License Tool is provided using an Azure AD account. This apoplication should be configured as it's own 
Enterprise Application within Azure AD. The service principal should utilise a certificate for authentication, and be should be configured 
as an owner on the group controlling access to the Genesys Cloud instances.

The application uses a configured credentials within Genesys Cloud instance with the appropriate permission to delete the tokens from the
active user when the license is returned.
