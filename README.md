[deprecated, please switch to the python-based client for pydio 6 and later]

# Pydio Java Sync

This application is a Java-based desktop tool that will allow you to seemlessly synchronize the content of a local folder to a remote Pydio repository. See http://pyd.io/ for more info about the core project. You can configure many synchronization tasks between various repositories of various ajaxplorer instances, to be synced to different folders on your computer. 

Network latency is reduced by using librsync on both end. Thus you MUST install the php_rsync extension on your server, along with the meta.filehasher plugin (see below). On the client computer, you must install a Java Runtime Environnment 1.6 or higher.

Please be aware that the application is not yet considered totally stable, thus you should really backup your data on a regular basis in case something went wrong! But still try to use it on a "real-life" set of data to have a good overview of how the client handles with high volumes of data and frequently changing.

## Server requirements : AjaXplorer v4.2.0+ & PHP rsync extension / Pydio 5.X

To get a low bandwith consumption, you should enable the use of the rsync algorithm on the server. This is done by installating the *PECL Rsync extension* (http://pecl.php.net/package/rsync), that interfaces PHP with the librsync functions (namely rdiff and all its declinaisons). See below for further instructions for Debian 6, CentOS 5 and CentOS 6.
Please report if you could successfully compile it on other distributions. 
Windows Server users, we will add some link to rdiff.exe , but it's not done yet.

Once this is installed, make sure that the repository has the "Meta Source" *meta.filehasher* enabled.

This extension will probably made optional in the next release, but it is not automatically advertised by the server yet, so the client consider it as active.

## Client installation 

Installers Downloads will be made available on Sourceforge, please go to https://sourceforge.net/projects/ajaxplorer/files/

Select the appropriate installer it should install everything needed on your computer :

* Windows : XP, Vista 7, 32 or 64 bits should be automatically detected
* Mac OS : Tested on 10.7.4, but will be more dependant on the java version probably.
* Linux : Not yet supported, should be quite straightforward to launch as it's nothing more than a Jar, but lately problems where found with the SWT SystemTray used not being displayed on Ubuntu.

Java Runtime Environnement must be at least 1.6 ( = "Java 6"), see http://java.com/
The Windows installer is based on a test version of Advanced Installer, thus it will trigger an alarm on install and uninstall, you can safely ignore this.

## Installing from night builds

The JAR's are built everynight, if you are invited by a team member to update them, you must first have the application originally installed with the installer, be it on Windows or Mac. Then you will find the correct package corresponding to your platform OS on http://ajaxplorer.info/download/ (tab "Nightly Builds", section "Synchro Client Package"), download and unzip the content somewhere. 

* On Mac, you have to right click the "Pydio" application icon and select "Display package content", then browse to Contents / Resources / Java. 
* On Windows, using windows Explorer, browse to the Abstrium > Pydio Sync program folder

Now in both cases, backup the PydioSync.jar and PydioSync_lib folder, and replace them by the content of the zip. The new JAR will be named something like pydio-sync-X.Y.Z-SNAPSHOT.jar, rename it to PydioSync.jar. The folder should already have the right name. That's it.

## Configuring the client

At first start, the client will open a dialog for creating a synchronization task. Fill in the form with the current values : 

* *Host* : exactly the same URL (starting with http:// or https://) as you are using to access Pydio through the web
* *User* & *Password* : Pydio user name & password
* *Repository* : once the previous fields are filled, click on the "load" button to get the list of repositories from the server, and select the one you want to synchronize
* *Local folder* : If not already pre-filled, browse your computer and choose the folder to which it will be synchronized.

## External Librarie used

### Rsync & Rdiff

The network delta minimisation relies on the Librsync project (LGPL), the source can be found on Sourceforge : https://sourceforge.net/projects/librsync/

### Quartz Scheduler

See http://quartz-scheduler.org/

## Installing Rsync Extension

### Install Rsync on Debian 6

Install librsync

> apt-get install librsync-dev

Install compilation tools

> apt-get install php5-dev
> apt-get install make

Install PECL Rsync extension

> pecl install channel://pecl.php.net/rsync-0.1.0

Add extension=rsync.so in PHP.INI
Restart Apache

### Install Rsync on CentOS6

Install librsync

> yum install librsync-devel

Compilation tools

> yum install php-devel
> yum install php-pear (to have pecl installed)
> yum install gcc

Install PECL Rsync extension

> pecl install channel://pecl.php.net/rsync-0.1.0

Add extension=rsync.so in PHP.INI
Restart Apache

### Install Rsync on CentOS5

We have to upgrade PHP to a more recent version than default CentOS (5.1.6)
To do this, 

> yum remove php php-*

Then 

> yum install php53 php53-cli php53-devel php53-gd  php53-mbstring php53-mysql php53-pdo php53-xml php53-xmlrpc php-pear

Install librsync

Add RPMForge to your repositories : 

Download the correct RPM from http://repoforge.org/use/
Then install it with rpm -Uvh rpmforge.blabla.rpm
Now you should be able to install librsync-devel

> yum install librsync-devel

Compilation tools

> yum install gcc

Install PECL Rsync extension

> pecl install channel://pecl.php.net/rsync-0.1.0

Add extension=rsync.so in PHP.INI
Restart Apache


## Contributing

Please <a href="http://pyd.io/contribute/cla">sign the Contributor License Agreement</a> before contributing.
