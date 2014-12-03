/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.utils;

import com.bric.util.JVM;
import pixelitor.GlobalKeyboardWatch;
import pixelitor.PixelitorWindow;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public abstract class OKCancelDialog extends JDialog {
    JComponent formPanel;
    JLabel messageLabel;
    private JScrollPane scrollPane;
    private JButton okButton;

    protected OKCancelDialog(JComponent form, String title) {
        this(form, PixelitorWindow.getInstance(), title, "OK", "Cancel");
    }

    protected OKCancelDialog(JComponent form, Frame owner, String title) {
        this(form, owner, title, "OK", "Cancel");
    }

    protected OKCancelDialog(JComponent form, Frame owner, String title, String okText, String cancelText) {
        this(form, owner, title, okText, cancelText, true);
    }

    protected OKCancelDialog(JComponent form, Frame owner, String title, String okText, String cancelText, boolean addScrollBars) {
        super(owner, title, true);
        this.formPanel = form;

        setLayout(new BorderLayout());

        addForm(form, addScrollBars);

        JPanel southPanel = new JPanel();


        okButton = new JButton(okText);
        JButton cancelButton = new JButton(cancelText);

        GlobalKeyboardWatch.setShowHideAllForTab(false);

        if (JVM.isMac) {
//        if(2 > 1) {
            southPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            southPanel.add(cancelButton);
            southPanel.add(okButton);
        } else {
            southPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            southPanel.add(okButton);
            southPanel.add(cancelButton);
        }

        add(southPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dialogAccepted();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    dialogCanceled();
                } catch (Exception ex) {
                    Dialogs.showExceptionDialog(ex);
                }
            }
        });

        // cancel when window is closed
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // the user pressed the X button...

                dialogCanceled();

            }
        });

        // cancel when ESC is pressed
        ((JComponent) getContentPane()).registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialogCanceled();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        pack();
        GUIUtils.centerOnScreen(this);
    }

    public void setOKButtonEnabled(boolean b) {
        okButton.setEnabled(b);
    }

    /**
     * Subclasses should always call super.dialogAccepted() and (if desired) dispose()
     */
    protected void dialogAccepted() {
        // TODO this is bad design: setShowHideAllForTab is called ven if the
        // dialog is not closed
        // Instead the dialog should listen itself for closing events and set
        // there
        // However if the user clicks on X, the WindowAdapter/windowClosed WILL be called
        // but for dispose not....
        // TODO anyway it is an error to ALWAYS call super.dialogAccepted() because the
        // window might not be closed
        // The point is that setShowHideAllForTab and dispose should be called AT THE SAME TIME

        // turn on again the global keyboard shortcut for Tab
        GlobalKeyboardWatch.setShowHideAllForTab(true);

    }

    /**
     * Subclasses should always call super.dialogCanceled() and dispose()
     */
    protected void dialogCanceled() {
        GlobalKeyboardWatch.setShowHideAllForTab(true);
    }

    public void setHeaderMessage(String message) {
        if(messageLabel != null) { // there was a message before
            remove(messageLabel);
        }
        messageLabel = new JLabel(message);
        add(messageLabel, BorderLayout.NORTH);
        revalidate();
    }

    public void changeFormPanel(JPanel panel) {
        if(scrollPane != null) {
            remove(scrollPane);
        } else {
            remove(formPanel);
        }
        formPanel = panel;
        addForm(formPanel, true);
        revalidate();
    }

    private void addForm(JComponent form, boolean addScrollBars) {
        if (addScrollBars) {
            scrollPane = new JScrollPane(form, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane, BorderLayout.CENTER);
        } else {
            add(form, BorderLayout.CENTER);
            scrollPane = null; // so that we later know that we have to remove from the root
        }
    }

}
