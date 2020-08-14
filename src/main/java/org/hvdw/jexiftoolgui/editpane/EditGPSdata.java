package org.hvdw.jexiftoolgui.editpane;

import org.hvdw.jexiftoolgui.MyVariables;
import org.hvdw.jexiftoolgui.Utils;
import org.hvdw.jexiftoolgui.controllers.CommandRunner;
import org.hvdw.jexiftoolgui.facades.IPreferencesFacade;
import org.hvdw.jexiftoolgui.facades.SystemPropertyFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.hvdw.jexiftoolgui.facades.SystemPropertyFacade.SystemPropertyKey.LINE_SEPARATOR;


public class EditGPSdata {

    private final static Logger logger = LoggerFactory.getLogger(EditGPSdata.class);
    private IPreferencesFacade prefs = IPreferencesFacade.defaultInstance;
    // I had specified for the arrays:
    //textfields:  gpsLatDecimaltextField, gpsLonDecimaltextField, gpsAltDecimaltextField, gpsLocationtextField, gpsCountrytextField, gpsStateProvincetextField, gpsCitytextField
    //checkboxes:  SaveLatLonAltcheckBox, gpsAboveSealevelcheckBox, gpsLocationcheckBox, gpsCountrycheckBox, gpsStateProvincecheckBox, gpsCitycheckBox, gpsBackupOriginalscheckBox

    public void setFormattedFieldMasks(JFormattedTextField[] gpsNumFields, JFormattedTextField[] gpsCalcFields) {
        Locale currentLocale = Locale.getDefault();
        NumberFormat latformatter = NumberFormat.getNumberInstance(currentLocale );
        // Latitude 0-90
        latformatter.setMaximumIntegerDigits(2);
        latformatter.setMaximumFractionDigits(6);
        gpsNumFields[0].setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(latformatter)));
        NumberFormat lonformatter = NumberFormat.getNumberInstance(currentLocale );
        // Longitude 0-180
        lonformatter.setMaximumIntegerDigits(3);
        lonformatter.setMaximumFractionDigits(6);
        gpsNumFields[1].setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(lonformatter)));
        //Altitude
        NumberFormat altformatter = NumberFormat.getNumberInstance(currentLocale );
        altformatter.setMaximumIntegerDigits(5);
        altformatter.setMaximumFractionDigits(2);
        gpsNumFields[2].setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(altformatter)));
        //GPS calc fields
        NumberFormat degminsecformatter = NumberFormat.getNumberInstance(currentLocale );
        degminsecformatter.setMaximumIntegerDigits(2);
        degminsecformatter.setMaximumFractionDigits(0);
        for (JFormattedTextField field : gpsCalcFields) {
            field.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(degminsecformatter)));
        }

        /*try {
            MaskFormatter latlonmask = new MaskFormatter("##.########");
            MaskFormatter altmask = new MaskFormatter("#####.##");
            MaskFormatter degminsecmask = new MaskFormatter("##");
            gpsNumFields[0] = new JFormattedTextField(latlonmask);
            gpsNumFields[1] = new JFormattedTextField(latlonmask);
            gpsNumFields[2] = new JFormattedTextField(altmask);

        } catch (ParseException e) {
            e.printStackTrace();
        } */
    }

    public void resetFields(JFormattedTextField[] gpsNumFields, JTextField[] gpsLocationFields) {

        for (JFormattedTextField field: gpsNumFields) {
            field.setText("");
        }
        for (JTextField field: gpsLocationFields) {
            field.setText("");
        }
    }

    public void copyGPSFromSelected(JFormattedTextField[] gpsNumFields, JTextField[] gpsLocationFields, JCheckBox[] gpsBoxes) {
        File[] files = MyVariables.getSelectedFiles();
        int SelectedRow = MyVariables.getSelectedRow();
        // Use "-n" for numerical values, like GPSAltitudeRef 0/1, instead of Above Sea Level/Below Sea Level
        String[] gpscopyparams = {"-e","-n","-exif:GPSLatitude","-exif:GPSLongitude","-exif:GPSAltitude","-exif:GPSAltitudeRef","-xmp:Location","-xmp:Country","-xmp:State","-xmp:City"};
        String fpath ="";
        String res = "";
        List<String> cmdparams = new ArrayList<String>();

        //First clean the fields
        resetFields(gpsNumFields, gpsLocationFields);

        if (Utils.isOsFromMicrosoft()) {
            fpath = files[SelectedRow].getPath().replace("\\", "/");
        } else {
            fpath = files[SelectedRow].getPath();
        }
        cmdparams.add(Utils.platformExiftool());
        cmdparams.addAll( Arrays.asList(gpscopyparams));
        cmdparams.add(fpath);
        try {
            res = CommandRunner.runCommand(cmdparams);
            logger.info("res is\n{}", res);
        } catch(IOException | InterruptedException ex) {
            logger.debug("Error executing command");
        }
        if (res.length() > 0) {
            displayCopiedInfo(gpsNumFields, gpsLocationFields, gpsBoxes, res);
        }
    }

    public void displayCopiedInfo(JFormattedTextField[] gpsNumFields, JTextField[] gpsLocationFields, JCheckBox[] gpsBoxes, String exiftoolInfo) {
        String[] lines = exiftoolInfo.split(SystemPropertyFacade.getPropertyByKey(LINE_SEPARATOR));
        for (String line : lines) {
            String[] cells = line.split(":", 2); // Only split on first : as some tags also contain (multiple) :
            String SpaceStripped = cells[0].replaceAll("\\s+","");  // regex "\s" is space, extra \ to escape the first \
            //Wit ALL spaces removed from the tag we als need to use identiefiers without spaces
            logger.info(SpaceStripped + "; value: " + cells[1], "\n");
            if (SpaceStripped.contains("Latitude")) {
                gpsNumFields[0].setText(cells[1].trim());
            }
            if (SpaceStripped.contains("Longitude")) {
                gpsNumFields[1].setText(cells[1].trim());
            }
            if ("GPSAltitude".equals(SpaceStripped)) {
                gpsNumFields[2].setText(cells[1].trim());
            }
            if (SpaceStripped.contains("AltitudeRef")) {
                if (cells[1].contains("0")) {
                    gpsBoxes[1].setSelected(true);
                } else {
                    gpsBoxes[1].setSelected(false);
                }
            }
            if (SpaceStripped.contains("Location")) {
                gpsLocationFields[0].setText(cells[1].trim());
            }
            if (SpaceStripped.contains("Country")) {
                gpsLocationFields[1].setText(cells[1].trim());
            }
            if (SpaceStripped.contains("State")) {
                gpsLocationFields[2].setText(cells[1].trim());
            }
            if (SpaceStripped.contains("City")) {
                gpsLocationFields[3].setText(cells[1].trim());
            }
        }

    }


    public void writeGPSTags(JFormattedTextField[] gpsNumFields, JTextField[] gpsLocationFields, JCheckBox[] gpsBoxes, JProgressBar progressBar) {

        int selectedIndices[] = MyVariables.getSelectedFilenamesIndices();
        File[] files = MyVariables.getSelectedFiles();
        List<String> cmdparams = new ArrayList<String>();

        cmdparams.add(Utils.platformExiftool());
        if (!gpsBoxes[6].isSelected()) { // default overwrite originals, when set do not
            cmdparams.add("-overwrite_original");
        }

        if (gpsBoxes[0].isSelected()) { // LatLonAlt
            // Exiftool prefers to only set one tag (exif or xmp) and retrieve with composite,
            // but I prefer to set both to satisfy every user
            cmdparams.add("-exif:GPSLatitude=" + gpsNumFields[0].getText().trim());
            if (Float.parseFloat(gpsNumFields[0].getText().trim()) > 0 ) {
                cmdparams.add("-exif:GPSLatitudeREF=N");
            } else {
                cmdparams.add("-exif:GPSLatitudeREF=S");
            }
            cmdparams.add("-exif:GPSLongitude=" + gpsNumFields[1].getText().trim());
            if (Float.parseFloat(gpsNumFields[1].getText().trim()) > 0 ) {
                cmdparams.add("-exif:GPSLongitudeREF=E");
            } else {
                cmdparams.add("-exif:GPSLongitudeREF=W");
            }
            cmdparams.add("-exif:GPSAltitude=" + gpsNumFields[2].getText().trim());
            cmdparams.add("-xmp:GPSLatitude=" + gpsNumFields[0].getText().trim());
            cmdparams.add("-xmp:GPSLongitude=" + gpsNumFields[1].getText().trim());
            cmdparams.add("-xmp:GPSAltitude=" + gpsNumFields[2].getText().trim());
            if (gpsBoxes[1].isSelected()) { //Altitude positive
                cmdparams.add("-exif:GPSAltitudeREF=above");
            } else {
                cmdparams.add("-exif:GPSAltitudeREF=below");
            }
        }
        // Again: exiftool prefers to only set one tag, but I set both
        if (gpsBoxes[2].isSelected()) {
            cmdparams.add("-xmp:Location=" + gpsLocationFields[0].getText().trim());
            cmdparams.add("-iptc:Sub-location=" + gpsLocationFields[0].getText().trim());
        }
        if (gpsBoxes[3].isSelected()) {
            cmdparams.add("-xmp:Country=" + gpsLocationFields[1].getText().trim());
            cmdparams.add("-iptc:Country-PrimaryLocationName=" + gpsLocationFields[1].getText().trim());
        }
        if (gpsBoxes[4].isSelected()) {
            cmdparams.add("-xmp:State=" + gpsLocationFields[2].getText().trim());
            cmdparams.add("-iptc:Province-State=" + gpsLocationFields[2].getText().trim());
        }
        if (gpsBoxes[5].isSelected()) {
            cmdparams.add("-xmp:City=" + gpsLocationFields[3].getText().trim());
            cmdparams.add("-iptc:City=" + gpsLocationFields[3].getText().trim());
        }


        boolean isWindows = Utils.isOsFromMicrosoft();
        for (int index: selectedIndices) {
            //logger.info("index: {}  image path: {}", index,  files[index].getPath());
            if (isWindows) {
                cmdparams.add(files[index].getPath().replace("\\", "/"));
            } else {
                cmdparams.add(files[index].getPath());
            }
        }


        CommandRunner.runCommandWithProgressBar(cmdparams, progressBar);


    }
}
