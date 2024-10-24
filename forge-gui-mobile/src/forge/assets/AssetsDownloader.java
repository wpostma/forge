package forge.assets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import forge.gui.GuiBase;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.gui.FThreads;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.screens.SplashScreen;
import forge.util.FileUtil;

public class AssetsDownloader {
    public static final boolean SHARE_DESKTOP_ASSETS = true; //change to false to test downloading separate assets for desktop version

    private final static ImmutableList<String> downloadIgnoreExit = ImmutableList.of("Download", "Ignore", "Exit");
    private final static ImmutableList<String> downloadExit = ImmutableList.of("Download", "Exit");

    //if not sharing desktop assets, check whether assets are up to date
    public static void checkForUpdates(final SplashScreen splashScreen) {
        if (Gdx.app.getType() == ApplicationType.Desktop && SHARE_DESKTOP_ASSETS) { return; }

        final String versionString = Forge.getDeviceAdapter().getVersionString();
        final boolean isSnapshots = versionString.contains("SNAPSHOT");
        final String snapsURL = "https://downloads.cardforge.org/dailysnapshots/";
        final String releaseURL = "https://releases.cardforge.org/forge/forge-gui-android/";
        final String versionText = isSnapshots ? snapsURL + "version.txt" : releaseURL + "version.txt";

        splashScreen.getProgressBar().setDescription("Checking for updates...");

        String message;
        boolean connectedToInternet = Forge.getDeviceAdapter().isConnectedToInternet();
        if (connectedToInternet) {
            try {
                URL versionUrl = new URL(versionText);
                String version = FileUtil.readFileToString(versionUrl);
                String filename = "forge-android-" + version + "-signed-aligned.apk";
                String apkURL = isSnapshots ? snapsURL + filename : releaseURL + version + "/" + filename;
                if (!StringUtils.isEmpty(version) && !versionString.equals(version)) {
                    splashScreen.prepareForDialogs();

                    message = "A new version of Forge is available (" + version + ").\n" +
                            "You are currently on an older version (" + versionString + ").\n\n" +
                            "Would you like to update to the new version now?";
                    if (!Forge.getDeviceAdapter().isConnectedToWifi()) {
                        message += " If so, you may want to connect to wifi first. The download is around 12MB.";
                    }
                    if (SOptionPane.showConfirmDialog(message, "New Version Available", "Update Now", "Update Later", true, true)) {
                        String apkFile = new GuiDownloadZipService("", "update", apkURL,
                            Forge.getDeviceAdapter().getDownloadsDir(), null, splashScreen.getProgressBar()).download(filename);
                        if (apkFile != null) {
                            Forge.getDeviceAdapter().openFile(apkFile);
                            Forge.isMobileAdventureMode = Forge.advStartup;
                            Forge.exitAnimation(false);
                            return;
                        }
                        SOptionPane.showOptionDialog("Could not download update. " +
                                "Press OK to proceed without update.", "Update Failed", null, ImmutableList.of("Ok"));
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        //see if assets need updating
        if (GuiBase.isAndroid()) {
            FileHandle resDir = Gdx.files.absolute(ForgeConstants.RES_DIR);
            FileHandle assetsDir = Gdx.files.absolute(ForgeConstants.ASSETS_DIR);
            FileHandle advBG = Gdx.files.absolute(ForgeConstants.DEFAULT_SKINS_DIR).child(ForgeConstants.ADV_TEXTURE_BG_FILE);
            if (!advBG.exists()) {
                FileHandle deleteVersion = assetsDir.child("version.txt");
                if (deleteVersion.exists())
                    deleteVersion.delete();
                FileHandle deleteBuild = resDir.child("build.txt");
                if (deleteBuild.exists())
                    deleteBuild.delete();
            }
        }
        File versionFile = new File(ForgeConstants.ASSETS_DIR + "version.txt");
        if (!versionFile.exists()) {
            try {
                versionFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
                Forge.exitAnimation(false); //can't continue if this fails
                return;
            }
        }
        else if (versionString.equals(FileUtil.readFileToString(versionFile)) && FSkin.getSkinDir() != null) {
            return; //if version matches what had been previously saved and FSkin isn't requesting assets download, no need to download assets
        }

        splashScreen.prepareForDialogs(); //ensure colors set up for showing message dialogs

        boolean canIgnoreDownload = FSkin.getAllSkins() != null; //don't allow ignoring download if resource files haven't been previously loaded

        if (!connectedToInternet) {
            message = "Updated resource files cannot be downloaded due to lack of internet connection.\n\n";
            if (canIgnoreDownload) {
                message += "You can continue without this download, but you may miss out on card fixes or experience other problems.";
            }
            else {
                message += "You cannot start the app since you haven't previously downloaded these files.";
            }
            switch (SOptionPane.showOptionDialog(message, "No Internet Connection", null, ImmutableList.of("Ok"))) {
                default: {
                    if (!canIgnoreDownload) {
                        Forge.exitAnimation(false); //exit if can't ignore download
                    }
                }
            }
            return;
        }

        //prompt user whether they wish to download the updated resource files
        message = "There are updated resource files to download. " +
                "This download is around 50MB, ";
        if (Forge.getDeviceAdapter().isConnectedToWifi()) {
            message += "which shouldn't take long if your wifi connection is good.";
        }
        else {
            message += "so it's highly recommended that you connect to wifi first.";
        }
        final List<String> options;
        message += "\n\n";
        if (canIgnoreDownload) {
            message += "If you choose to ignore this download, you may miss out on card fixes or experience other problems.";
            options = downloadIgnoreExit;
        } else {
            message += "This download is mandatory to start the app since you haven't previously downloaded these files.";
            options = downloadExit;
        }

        switch (SOptionPane.showOptionDialog(message, "", null, options)) {
            case 1:
                if (!canIgnoreDownload) {
                    Forge.exitAnimation(false); //exit if can't ignore download
                }
                return;
            case 2:
                Forge.exitAnimation(false);
                return;
        }

        //allow deletion on Android 10 or if using app-specific directory
        boolean allowDeletion = Forge.androidVersion < 30 || GuiBase.isUsingAppDirectory();
        String assetURL = isSnapshots ? snapsURL + "assets.zip" : releaseURL + versionString + "/" + "assets.zip";
        new GuiDownloadZipService("", "resource files", assetURL,
            ForgeConstants.ASSETS_DIR, ForgeConstants.RES_DIR, splashScreen.getProgressBar(), allowDeletion).downloadAndUnzip();

        if (allowDeletion)
            FSkinFont.deleteCachedFiles(); //delete cached font files in case any skin's .ttf file changed

        //reload light version of skin after assets updated
        FThreads.invokeInEdtAndWait(() -> {
            FSkinFont.updateAll(); //update all fonts used by splash screen
            FSkin.loadLight(FSkin.getName(), splashScreen);
        });

        //save version string to file once assets finish downloading
        //so they don't need to be re-downloaded until you upgrade again
        FileUtil.writeFile(versionFile, versionString);

        //add restart after assets update
        String msg  = allowDeletion ? "Resource update finished..." : "Forge misses some files for deletion.\nIf you encounter issues, try deleting the Forge/res folder and/or deleting Forge/cache/fonts folder and try to download and update the assets.";
        switch (SOptionPane.showOptionDialog(msg, "", null, ImmutableList.of("Restart"))) {
            default:
                Forge.exitAnimation(true);
        }
    }
}
