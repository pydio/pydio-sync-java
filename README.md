# AjaXplorer In Sync

Please be aware that the application is not yet considered totally stable, thus you should really backup your data on a regular basis in case something went wrong! But still try to use it on a "real-life" set of data to have a good overview of how the client handles with high volumes of data and frequently changing.

## Server requirements : v4.2.0+ & PHP rsync extension

It is not mandatory, but to get a low bandwith consumption, you should enable the use of the rsync algorithm on the server. This is done by installating the *PECL Rsync extension* (http://pecl.php.net/package/rsync), that interfaces PHP with the librsync functions (namely rdiff and all its declinaisons). This is not so easy to install, but was currently successfully tested on Debian 6. Please report (by creating a "Feature" issue) if you could successfully install it on other distributions. 
Windows Server users, we will add some link to rdiff.exe , but it's not done yet.

Once this is installed, make sure that the repository has the "Meta Source" *meta.filehasher* enabled.

This extension will probably made optional in the next release, but it is not automatically advertised by the server yet, so the client consider it as active.

## Client installation 

Select the appropriate installer in the download page, it should install everything needed on your computer :

* Windows : XP, Vista 7, 32 or 64 bits should be automatically detected
* Mac OS : Tested on 10.7.4, but will be more dependant on the java version probably.

Java Runtime Environnement must be at least 1.6 ( = "Java 6"), see http://java.com/
The Windows installer is based on a test version of Advanced Installer, thus it will trigger an alarm on install and uninstall, you can safely ignore this.

## Configuring the client

At first start, the client will open a dialog for creating a synchronization task. Fill in the form with the current values : 

* *Host* : exactly the same URL (starting with http:// or https://) as you are using to access Ajaxplorer through the web
* *User* & *Password* : AjaXplorer user name & password
* *Repository* : once the previous fields are filled, click on the "load" button to get the list of repositories from the server, and select the one you want to synchronize
* *Local folder* : If not already pre-filled, browse your computer and choose the folder to which it will be synchronized.
