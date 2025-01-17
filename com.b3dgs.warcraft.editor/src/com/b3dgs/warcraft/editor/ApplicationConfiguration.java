/*
 * Copyright (C) 2013-2019 Byron 3D Games Studio (www.b3dgs.com) Pierre-Alexandre (contact@b3dgs.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.b3dgs.warcraft.editor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.b3dgs.lionengine.Medias;
import com.b3dgs.lionengine.Verbose;
import com.b3dgs.lionengine.editor.dialog.project.ProjectImportHandler;
import com.b3dgs.lionengine.editor.project.Project;
import com.b3dgs.lionengine.editor.project.ProjectFactory;
import com.b3dgs.lionengine.editor.utility.UtilPart;
import com.b3dgs.lionengine.editor.world.WorldModel;
import com.b3dgs.lionengine.editor.world.view.WorldPart;
import com.b3dgs.lionengine.game.feature.tile.TileRef;
import com.b3dgs.lionengine.game.feature.tile.map.MapTile;
import com.b3dgs.lionengine.game.feature.tile.map.MapTileAppender;
import com.b3dgs.lionengine.game.feature.tile.map.MapTileAppenderModel;
import com.b3dgs.lionengine.game.feature.tile.map.MapTileGroup;
import com.b3dgs.lionengine.game.feature.tile.map.transition.MapTileTransition;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.MapTileCircuit;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.generator.GeneratorParameter;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.generator.MapGenerator;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.generator.MapGeneratorImpl;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.generator.PrefMapFill;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.generator.PrefMapRegion;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.generator.PrefMapSize;
import com.b3dgs.lionengine.game.feature.tile.map.transition.circuit.generator.TileArea;

/**
 * Configure the editor with the right name.
 */
public class ApplicationConfiguration
{
    /** Import project argument. */
    private static final String ARG_IMPORT = "-import";

    /** Application reference. */
    @Inject private MApplication application;

    /**
     * Constructor.
     */
    public ApplicationConfiguration()
    {
        super();
    }

    /**
     * Execute the injection.
     * 
     * @param eventBroker The event broker service.
     */
    @PostConstruct
    public void execute(IEventBroker eventBroker)
    {
        final MWindow existingWindow = application.getChildren().get(0);
        existingWindow.setLabel(Activator.PLUGIN_NAME);
        eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new AppStartupCompleteEventHandler());
    }

    /**
     * Handler called on startup complete.
     */
    private class AppStartupCompleteEventHandler implements EventHandler
    {
        /**
         * Constructor.
         */
        AppStartupCompleteEventHandler()
        {
            super();
        }

        /**
         * Check if there is a project to import.
         */
        private void checkProjectImport()
        {
            final String[] args = Platform.getApplicationArgs();
            for (int i = 0; i < args.length; i++)
            {
                if (ApplicationConfiguration.ARG_IMPORT.equals(args[i]))
                {
                    i++;
                    if (i < args.length)
                    {
                        importProject(args[i]);

                        final MapTile map = WorldModel.INSTANCE.getMap();
                        map.create(Medias.create("map", "forest", "forest.png"));
                        map.getFeature(MapTileGroup.class).loadGroups(Medias.create("map", "forest", "groups.xml"));
                        map.getFeature(MapTileTransition.class)
                           .loadTransitions(Medias.create("map", "forest", "transitions.xml"));
                        map.getFeature(MapTileCircuit.class)
                           .loadCircuits(Medias.create("map", "forest", "circuits.xml"));

                        final GeneratorParameter parameters = new GeneratorParameter();
                        parameters.add(new PrefMapSize(16, 16, 64, 64))
                                  .add(new PrefMapFill(new TileRef(0, 0)))
                                  .add(new PrefMapRegion(new TileRef(0, 12), new TileArea(0, 0, 8, 64), 4, 60))
                                  .add(new PrefMapRegion(new TileRef(0, 12), new TileArea(56, 0, 64, 64), 4, 60))
                                  .add(new PrefMapRegion(new TileRef(0, 12), new TileArea(0, 0, 64, 8), 4, 60))
                                  .add(new PrefMapRegion(new TileRef(0, 12), new TileArea(0, 56, 64, 64), 4, 60))
                                  .add(new PrefMapRegion(new TileRef(0, 29), new TileArea(12, 12, 56, 56), 2, 250))
                                  .add(new PrefMapRegion(new TileRef(0, 12), new TileArea(24, 24, 40, 40), 2, 80))
                                  .add(new PrefMapRegion(new TileRef(0, 0), new TileArea(4, 4, 60, 60), 1, 100));

                        final MapGenerator generator = new MapGeneratorImpl();
                        final MapTileAppender appender = map.addFeatureAndGet(new MapTileAppenderModel(WorldModel.INSTANCE.getServices()));
                        appender.append(generator.generateMap(parameters,
                                                              Arrays.asList(Medias.create("map",
                                                                                          "forest",
                                                                                          "forest.png")),
                                                              Medias.create("map", "forest", "sheets.xml"),
                                                              Medias.create("map", "forest", "groups.xml")),
                                        0,
                                        0);
                        UtilPart.getPart(WorldPart.ID, WorldPart.class).update();
                    }
                }
            }
        }

        /**
         * Import a project from a path.
         * 
         * @param projectPath The project path.
         */
        private void importProject(String projectPath)
        {
            final File path = new File(projectPath);
            try
            {
                final Project project = ProjectFactory.create(path.getCanonicalFile());
                ProjectImportHandler.importProject(project);
            }
            catch (final IOException exception)
            {
                Verbose.exception(exception);
            }
        }

        @Override
        public void handleEvent(Event event)
        {
            checkProjectImport();
        }
    }
}
