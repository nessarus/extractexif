package com.blogspot.nessarus47.extractexif;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class LongLat {
	public static String saveImage(String imageUrl) throws IOException {
		URL url = new URL(imageUrl);
		String fileName = url.getFile();
		String destName = fileName.substring(fileName.lastIndexOf("/") + 1);
		System.out.println(destName);

		InputStream input = url.openStream();
		OutputStream output = new FileOutputStream(destName);

		byte[] store = new byte[2048];
		int length;

		while((length = input.read(store)) != -1) {
			output.write(store, 0, length);
		}

		input.close();
		output.close();
		return destName;
	}

	public static ArrayList<String> CSVurl(String path, int noOfEntries) {
		ArrayList<String> ret = new ArrayList<String>();
		try { 
			FileReader filereader = new FileReader(path);

			CSVReader csvReader = new CSVReader(filereader);
			String[] nextRow;

			csvReader.readNext();
			// we are going to read data line by line 
			int count = 0;
			while ((nextRow = csvReader.readNext()) != null) {
				ret.add(nextRow[0]);
				if (noOfEntries > 0 && count > noOfEntries - 2) {
					break;
				}
				count++;
			}
			csvReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static ArrayList<String[]> exifExtract(ArrayList<String> urls, String parentPath) throws ImageProcessingException, IOException {
		ArrayList<String[]> ret = new ArrayList<String[]>();
		for (int i = 0; i < urls.size(); i++) {
			File file = new File(parentPath, saveImage(urls.get(i)));

			Metadata metadata = ImageMetadataReader.readMetadata(file);

			if (metadata.containsDirectoryOfType(GpsDirectory.class)) {
				GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
				GeoLocation location = gpsDirectory.getGeoLocation();
				String[] latlong = { Double.toString(location.getLatitude()), Double.toString(location.getLongitude()) };
				ret.add(latlong);
			} else {
				String[] latlong = {"None", "None"};
				ret.add(latlong);
			}
		}
		return ret;
	}

	public static void updateCSV(String path, String parentPath, ArrayList<String[]> locations) throws IOException {
		File newfile = new File(parentPath, path.substring(parentPath.length() + 1, path.length() - 4) + " Updated.csv");
		newfile.createNewFile();
	    try { 
	        FileWriter outputfile = new FileWriter(newfile); 
	        
	        CSVWriter writer = new CSVWriter(outputfile); 
	        
	        FileReader filereader = new FileReader(path);
	        CSVReader csvReader = new CSVReader(filereader);
	        
	        ArrayList<String[]> data = new ArrayList<String[]>(); 
	        String[] nextRow;
	        int count = 0;
	        nextRow = csvReader.readNext();
	        nextRow[0] = nextRow[0].substring(1, nextRow[0].length());
	        data.add(nextRow);
	        while ((nextRow = csvReader.readNext()) != null) {
	        	nextRow[9] = locations.get(count)[0];
				nextRow[10] = locations.get(count)[1];
	        	print(nextRow);
	        	data.add(nextRow);
	        	count++;
	        	if(count == locations.size()) {
	        		break;
	        	}
	        }
	        
	        writer.writeAll(data); 
	        writer.close(); 
	        csvReader.close();
	    } 
	    catch (IOException e) { 
	        e.printStackTrace(); 
	    } 
	}
	
	public static void print(String[] g) {
		for(int i = 0; i < g.length; i++) {
			System.out.print(g[i]);
		}
		System.out.println();
	}

	public static void main(String[] args) throws IOException, ImageProcessingException, URISyntaxException {		
		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV FILES", "csv");
		jfc.setFileFilter(filter);
		int success = jfc.showOpenDialog(null);
		String csvPath = "";
		String csvParentPath = "";
		if (success == JFileChooser.APPROVE_OPTION) {
			File chosenFile = jfc.getSelectedFile();
			csvPath = chosenFile.getAbsolutePath();
			csvParentPath = chosenFile.getParentFile().getAbsolutePath();
		} else if(success == JFileChooser.CANCEL_OPTION) {
			return;
		}

		String userInputNumber = JOptionPane.showInputDialog("Please input number of entries to process: ");
		if(userInputNumber == null) {
			return;
		}
		int noOfEntries;
		while(true) {
		    try {
		    	if(userInputNumber.length()==0) {
		    		noOfEntries = 0;
		    	} else {
		    		noOfEntries = Integer.parseInt(userInputNumber);		    		
		    	}
		    	break;
		    }
		    catch( Exception e ) {
		    	userInputNumber = JOptionPane.showInputDialog("Please input number of entries to process: ");
				if(userInputNumber == null) {
					return;
				}
		    }
		}
		
		ArrayList<String> urls = CSVurl(csvPath, noOfEntries);
		for (int i = 0; i < urls.size(); i++) {
			System.out.println(urls.get(i));
		}

		ArrayList<String[]> locations = exifExtract(urls, csvParentPath);
		for (int i = 0; i < urls.size(); i++) {
			System.out.println(locations.get(i)[0] + ", " + locations.get(i)[1]);
		}

		updateCSV(csvPath, csvParentPath, locations);

	}
}