package textCreation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfCreationService extends PdfPageEventHelper {

	public static void createAPdf(String title, String author, String textContent, double elapsedSeconds)
			throws DocumentException, MalformedURLException, IOException {

		// get date for filename
		final String currentDate = Calendar.getInstance().getTime().toString();
		SimpleDateFormat sdff = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
		Date now = new Date();
		String filenameDate = sdff.format(now);

		// create file
		String filenameTitle = title.replace("/n", "_").replace(".", "_").replace(",", "-").replace(" ", "-")
				.replace("--", "-").replace("__", "_").replace("-_", "_").replace("_-", "_").replace("/n", "_")
				.replace("*", " ").replace("//", " ").replace("\"", " ").replace("[", " ").replace("]", " ")
				.replace(":", " ").replace(";", " ").replace("<", " ").replace(">", " ").replace("?", " ")
				.replace("\\", " ");
		File file = new File("Books/" + filenameTitle + "_" + filenameDate + ".pdf");
		file.delete();
		
		// create Pdf file
		Document document = new Document(PageSize.A4);
		PdfWriter writer = null;
		try {
			writer = PdfWriter.getInstance(document, new FileOutputStream(file));
		} catch (Exception e) {
			writer = PdfWriter.getInstance(document, new FileOutputStream(new File("Books/"
					+ (ThreadLocalRandom.current().nextInt(1000, 100000 + 1)) + "_" + filenameDate + ".pdf")));
		}
		document.open();
		document.addAuthor("GerH");

		// author font
		Font ffont = new Font(Font.FontFamily.UNDEFINED, 9, Font.ITALIC);
		// title font
		Font tfont = new Font(Font.FontFamily.UNDEFINED, 40, Font.BOLD);
		// small title font
		Font stfont = new Font(Font.FontFamily.UNDEFINED, 15, Font.BOLD);
		// disclaimer font
		Font dfont = new Font(Font.FontFamily.UNDEFINED, 9);
		// small title font
		//Font nfont = new Font(Font.FontFamily.UNDEFINED, 12, Font.NORMAL);

		// Image
		String imagePath = "/Logo_icon_msg.png";
		URL imageURL = PdfCreationService.class.getResource(imagePath);
		Image img = Image.getInstance(imageURL);
		img.scaleAbsolute(80f, 80f);
		img.setAlignment(Element.ALIGN_CENTER);
		
		// define paragraphs
		Paragraph prefaceChapters = new Paragraph("\n\n"+title+"\n\n\n", stfont);
		prefaceChapters.setAlignment(Element.ALIGN_CENTER);
		Paragraph preface1 = new Paragraph("\n\n\n" + title, tfont);
		preface1.setAlignment(Element.ALIGN_CENTER);
		Paragraph preface2 = new Paragraph("\ncreated on " + currentDate, ffont);
		preface2.setAlignment(Element.ALIGN_CENTER);
		Paragraph preface3 = new Paragraph("by " + author + "\n\n\n", ffont);
		preface3.setAlignment(Element.ALIGN_CENTER);
		Paragraph preface4 = new Paragraph(
				"\nThis book was created by 'Synthekst'. \nSynthekst is a program made by GerH.", dfont);
		preface4.setAlignment(Element.ALIGN_CENTER);

		Paragraph prefaceDisclaimer = new Paragraph(
				"\n\n\nThis book is sold subject to the condition that it shall not, by way\nof trade or otherwise, be lent, re-sold, hired out, or otherwise\ncirculated without the publisher's prior consent in any form of\nbinding or cover other than that in which it is published and\nwithout a similar condition including this condition being\nimposed on the subsequent purchaser.");
		prefaceDisclaimer.setAlignment(Element.ALIGN_CENTER);
		Paragraph prefaceEnd = new Paragraph("\n\n\n\n\nThe End", tfont);
		prefaceEnd.setAlignment(Element.ALIGN_CENTER);

		// Document
		document.open();
		document.add(preface1);
		document.add(preface2);
		document.add(preface3);
		document.add(img);
		document.add(preface4);
		document.add(Chunk.NEWLINE);
		document.newPage();
		document.add(prefaceDisclaimer);
		document.newPage();
		document.add(prefaceChapters);
		document.add(new Paragraph(textContent));
		document.newPage();
		document.newPage();
		document.add(prefaceEnd);
		document.close();
		writer.close();
		
		//add page numbers
		try {
			PageNumberService.manipulatePdf(file, elapsedSeconds);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
}