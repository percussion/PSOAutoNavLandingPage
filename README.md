PSOAutoNavLandingPage
=====================

This extensions will automatically associate a item (defined as a landing page in a configuration file) to the Navon or NavTree residing in the folder it was created in.  If the Navon or NavTree already has a Landing Page, then no action is taken.

Prerequisite
Ant needs to be installed on the server to install the extensions

Steps
1. Shutdown the server
2. Unzip the PSONavAutoLandingPage file into the Rhythmyx directory
3. Run installExtensions.bat
4. Start the server
5. Log into the Workbench
6. Switch to the System Design perspective
7. Under Relationship Types -> System
8. Open the FolderContent Relationship
9. Using the tabs on the bottom, switch to Effects


 
10. Replace the rxs_NavFolderEffect() with the new PSONavFolderEffect() leaving the other parameters the same
11. Modify the file: rxconfig/Server/landingPageCombinations.xml config to specify the Content Type IDs and Template Names desired as landing pages
12. Restart the server