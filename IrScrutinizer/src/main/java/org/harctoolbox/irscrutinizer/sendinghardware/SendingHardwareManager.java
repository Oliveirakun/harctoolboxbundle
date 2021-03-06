/*
Copyright (C) 2013 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.irscrutinizer.sendinghardware;

import java.awt.Component;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.guicomponents.GuiUtils;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;
import org.harctoolbox.irscrutinizer.Props;

/**
 * This class does something interesting and useful. Or not...
 */
public class SendingHardwareManager {
    private final LinkedHashMap<String, ISendingHardware<?>> table;
    private final JTabbedPane tabbedPane;
    private JMenu menu;
    private ButtonGroup buttonGroup;
    private final GuiUtils guiUtils;
    private final Props properties;
    private ISendingHardware<?> selected;

    public SendingHardwareManager(GuiUtils guiUtils, Props properties, JTabbedPane tabbedPane) {
        this.selected = null;
        this.menu = null;
        this.guiUtils = guiUtils;
        this.properties = properties;
        this.tabbedPane = tabbedPane;
        table = new LinkedHashMap<>(16);
    }

    public Collection<ISendingHardware<?>> getSendingHardware() {
        return table.values();
    }

    public void add(ISendingHardware<?> sender) {
        if (table.containsKey(sender.getName()))
            throw new IllegalArgumentException(sender.getName() + " already exists in map.");
        table.put(sender.getName(), sender);
    }

    public String[] getNames() {
        return table.keySet().toArray(new String[table.size()]);
    }

    public ISendingHardware<?> getSendingHardware(String name) {
        return table.get(name);
    }

    public String getSelectedHardwareName() {
        return selected.getName();
    }

    public void setVerbose(boolean verbose) {
        selected.setVerbose(verbose);
    }

    public boolean sendIr(IrSignal irSignal, int count) throws IOException, IrpMasterException, NoSuchTransmitterException, HardwareUnavailableException, HarcHardwareException {
        if (selected == null)
            throw new HardwareUnavailableException("No transitting hardware selected.");
        return selected.sendIr(irSignal, count);
    }

    private void createMenu(String selection) {
        menu = new JMenu();
        menu.setText("Transmitting Hardware");
        menu.setToolTipText("Allows direct selection of transmitting hardware");
        buttonGroup = new ButtonGroup();
        table.entrySet().stream().map((kvp) -> {
            String name = kvp.getKey();
            final ISendingHardware<?> hardware = kvp.getValue();
            JRadioButton menuItem = new JRadioButton(name);
            menuItem.setSelected(name.equals(selection));
            //portRadioButtons[i] = menuItem;
            menuItem.addActionListener((java.awt.event.ActionEvent evt) -> {
                try {
                    select(hardware);
                } catch (HarcHardwareException ex) {
                    guiUtils.error(ex);
                }
            });
            return menuItem;
        }).map((menuItem) -> {
            buttonGroup.add(menuItem);
            return menuItem;
        }).forEachOrdered((menuItem) -> {
            menu.add(menuItem);
        });
    }

    public JMenu getMenu() {
        if (menu == null)
            createMenu(selected != null ? selected.getName() : null);
        return menu;
    }

    /**
     *
     * @param name
     * @throws HarcHardwareException
     */
    public void select(String name) throws HarcHardwareException {
        ISendingHardware<?> hardware = table.get(name);
        if (hardware == null)
            //throw new IllegalArgumentException(name + " does not exist in map.");
            return;

        select(hardware);
    }

    private void select(ISendingHardware<?> hardware) throws HarcHardwareException {
        // invokes selectHardware through capturingHardwareTabbedPaneStateChanged
        tabbedPane.setSelectedComponent(hardware.getPanel()); // throws IllegalArgumentException
        if (selected == null) // if capturingHardwareTabbedPaneStateChanged did not invoke selectDoWork
            selectDoWork(hardware);
    }

    /**
     *
     * @param name
     * @throws HarcHardwareException
     */
    public void selectDoWork(String name) throws HarcHardwareException {
        ISendingHardware<?> hardware = table.get(name);
        if (hardware == null)
            //throw new IllegalArgumentException(name + " does not exist in map.");
            return;

        selectDoWork(hardware);
    }

    /**
     *
     * @param hardware
     * @throws HarcHardwareException
     */
    private void selectDoWork(ISendingHardware<?> hardware) throws HarcHardwareException {
        selected = null;
        try {
            hardware.setup();
            hardware.setVerbose(properties.getVerbose());
            selected = hardware;
            properties.setTransmitHardware(hardware.getName());
        } catch (IOException ex) {
            guiUtils.error(ex);
        }

        if (menu != null) {
            for (Component component : menu.getMenuComponents()) {
                JRadioButton menuItem = (JRadioButton) component;
                menuItem.setSelected(menuItem.getText().equals(hardware.getName()));
            }
        }
    }

    public void close() {
        table.values().forEach((hardware) -> {
            hardware.close();
        });
    }
}
