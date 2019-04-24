## eclipsetrader
![](https://github.com/eclipsequote/eclipsetrader/blob/master/001LcZePgy6WshwVqjQ4e.jpg)


Development Environment Setup
EclipseTrader is developed using Eclipse 3.8 (Juno).

Download and install Eclipse SDK from ​http://download.eclipse.org/eclipse/downloads/drops/R-3.8.1-201209141540/
The target platform is composed from the following packages:
eclipse-platform-SDK-3.8.1
eclipse-3.8.1-delta-pack.zip Unpack all packages to a dedicated directory, for example eclipse-platform-3.8. 
For your convenience, we have built the complete target platform package and made it available from the Downloads page.
From Eclipse, open Window -> Preferences and go to Plug-in Development / Target Platform and add a new target platform definition pointing to the directory location above. Make sure to select the new definition as the default (active) target platform.
The project uses ​Subversion as the source code repository, so you need to install an svn Team Provider. We are using ​Subclipse.
Download and import the source code.

Check-out from command line

Check-out the complete source code tree from the subversion repository:

svn co http://svn.eclipsetrader.org/svnroot/eclipsetrader/trunk eclipsetrader
This command checks-out all projects, including JUnit tests and the Nebula Widgets plugins to a directory named eclipsetrader.

Start Eclipse and point the workspace to the eclipsetrader directory (or wherever you have checked-out the source code). Import all projects using File -> Import -> General / Existing Projects into Workspace (leave the Copy projects into workspace option unchecked).

Check-out with Team Project Set

Download trunkProjectSet.psf​ and import it using File -> Import -> Team / Team Project Set. The import should create the subversion and cvs repositories and check-out the source code automatically.

Setup the Run Configuration

EclipseTrader is not meant to be run as a generic Eclipse plugin, it runs as a standalone RCP (Rich Client Platform) application. The source code includes a sample launch configuration:

From Eclipse open `Run -> Run Configurations'.
Expand Eclipse Application tree from the list at the left and select EclipseTrader.
If the Run button is disabled, switch to the other pages until it becomes active.
Click Run to run EclipseTrader.
Attachments (1)

trunkProjectSet.psf​ (9.0 KB) - added by admin on Dec 12, 2011 at 2:48:15 AM. Updated to current source tree
Download all attachments as: .zip
