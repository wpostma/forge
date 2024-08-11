/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view;

import forge.GuiDesktop;
import forge.Singletons;
import forge.error.ExceptionHandler;
import forge.gui.GuiBase;
import forge.gui.card.CardReaderExperiments;
import forge.util.BuildInfo;
import io.sentry.Sentry;

/**
 * Main class for Forge's swing application view.
 *
 *
 *
 *
 *
 */

/*

IDE launch parameters : JDK 17+

-Xms768m
-XX:+UseParallelGC
-Dsun.java2d.xrender=false
--add-opens
java.base/java.util=ALL-UNNAMED
--add-opens
java.base/java.lang=ALL-UNNAMED
--add-opens
java.base/java.lang.reflect=ALL-UNNAMED
--add-opens
java.base/java.text=ALL-UNNAMED
--add-opens
java.desktop/java.awt.font=ALL-UNNAMED
--add-opens
java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens
java.base/sun.nio.ch=ALL-UNNAMED
--add-opens
java.base/java.nio=ALL-UNNAMED
--add-opens
java.base/java.math=ALL-UNNAMED
--add-opens
java.base/java.util.concurrent=ALL-UNNAMED
--add-opens
java.desktop/java.awt=ALL-UNNAMED
--add-opens
java.base/java.net=ALL-UNNAMED
--add-opens
java.desktop/javax.swing=ALL-UNNAMED
--add-opens
java.desktop/java.beans=ALL-UNNAMED
--add-opens
java.desktop/javax.swing.border=ALL-UNNAMED
-Dio.netty.tryReflectionSetAccessible=true

*/

public final class Main {
    /**
     * Main entry point for Forge
     */
    public static void main(final String[] args) {


        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true);
            options.setRelease(BuildInfo.getVersionString());
            options.setEnvironment(System.getProperty("os.name"));
            options.setTag("Java Version", System.getProperty("java.version"));
            //options.setDsn("");
        }, true);



        // HACK - temporary solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        //Turn off the Java 2D system's use of Direct3D to improve rendering speed (particularly when Full Screen)
        System.setProperty("sun.java2d.d3d", "false");
        
        //Turn on OpenGl acceleration to improve performance
        //System.setProperty("sun.java2d.opengl", "True");

        //setup GUI interface
        GuiBase.setInterface(new GuiDesktop());

        //install our error handler
        ExceptionHandler.registerErrorHandling();

        int forgeArgs = 0;
        int i;
        for (i = 0; i<args.length;i++){
            if ( args[i].startsWith("-") ) {
                break;
            }
            forgeArgs = i+1;
            System.out.format("forge command line startup: %s ", args[i]);
        }
        // Start splash screen first, then data models, then controller.
        if (forgeArgs == 0) {
            Singletons.initializeOnce(true);

            // Controller can now step in and take over.
            Singletons.getControl().initialize();
            return;
        }

        // command line startup here
        String mode = args[0].toLowerCase();
        
        switch(mode) {
            case "sim":
                System.out.println("simulate match");
                SimulateMatch.simulate(args);
                break;

            case "parse":
            	CardReaderExperiments.parseAllCards(args);
                break;

            case "server":
                System.out.println("Dedicated server mode.\nNot implemented.");
                break;
            
            default:
                System.out.format("Unknown mode.\nKnown mode is 'sim', 'parse'. \n parameter:  %s \n\n", args[0] );
                break;
        }
        
        System.exit(0);
    }

    @SuppressWarnings("deprecation")
	@Override
    protected void finalize() throws Throwable {
        try {
            ExceptionHandler.unregisterErrorHandling();
        } finally {
            super.finalize();
        }
    }

    // disallow instantiation
    private Main() { }
}
