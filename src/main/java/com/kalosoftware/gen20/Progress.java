package com.kalosoftware.gen20;

import java.awt.Desktop;
import java.io.File;
import java.sql.Connection;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Progress extends SwingWorker<Integer, String> {

    private Form vista;

    public Progress(Form vista) {
        this.vista = vista;
    }

    @Override
    protected Integer doInBackground() throws Exception {
        this.vista.getBarra().setVisible(true);
        this.vista.getBarra().setStringPainted(true);
        this.vista.getDirectoryText().setEditable(false);
        this.vista.getProyectoText().setEditable(false);

        this.vista.getBarra().setValue(5);

        final String groupId = this.vista.getGroupText().getText();
        final String artifactId = this.vista.getProyectoText().getText();
        final String path = this.vista.getDirectoryText().getText() + "\\" + artifactId;
        
        this.vista.getBarra().setValue(10);

        Project.startStructure(this.vista.getDirectoryText().getText(), groupId, artifactId, this.vista.getUrlText().getText(), this.vista.getUsernameText().getText(), this.vista.getPassText().getText());
        
        this.vista.getBarra().setValue(15);

        final Generator gen = new Generator(this.vista.getDirectoryText().getText(), path, groupId, artifactId, this.vista.getCon());
        this.vista.getBarra().setValue(20);
        gen.start(this.vista.getUrlText().getText(), this.vista.getUsernameText().getText(), this.vista.getPassText().getText(), this.vista.getBarra());
        
        this.vista.getBarra().setString("Proyecto generado con exito");
        Desktop.getDesktop().open(new File(this.vista.getDirectoryText().getText() + "\\" + this.vista.getProyectoText().getText()));
        this.vista.getDirectoryText().setEditable(true);
        this.vista.getProyectoText().setEditable(true);
        return 0;
    }

}
