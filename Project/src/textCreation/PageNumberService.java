package textCreation;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import application.DialogBoxes;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PageNumberService {

	//add page numbers, beginning at page 3
	public static void manipulatePdf(File sourceFile, double elapsedSeconds) throws IOException, DocumentException {
		String source = sourceFile.getName();
		String dest = "Books/Book_"+source;
		File file = new File(dest);
		file.getParentFile().mkdirs();
		
		PdfReader reader = new PdfReader("Books/"+source);
		int n = reader.getNumberOfPages();
		PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
		PdfContentByte pagecontent;
		Font nfont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);
		
		for (int i = 2; i < n;) {
			pagecontent = stamper.getOverContent(++i);
			// page num gets added here:
			ColumnText.showTextAligned(pagecontent, Element.ALIGN_RIGHT,
					new Phrase(String.format("%s", i, n), nfont), 559, 20, 0);
		}
		stamper.close();
		reader.close();

		DialogBoxes.showMessageBox("Document Created!", "A pdf document with the name " + file.getName()
					+ " was created in " + elapsedSeconds + " seconds.", "Path: " + file.getAbsolutePath());
		
		//delete file without page numbers
		System.gc();
		sourceFile.delete();
	}
}