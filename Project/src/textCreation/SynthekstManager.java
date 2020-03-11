package textCreation;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import com.itextpdf.text.DocumentException;
import application.DialogBoxes;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SynthekstManager {

	/*
	 * Long processes:
	 * 
	 * 
	 * Text creation: create text -> convert2016 -> complete()[] -> add paragr ->
	 * lastTextFixes
	 * 
	 * Text creation: create text -> convert2016 -> complete()[] -> add paragr ->
	 * lastTextFixes -> makePdf
	 * 
	 * Text read in: clean up text -> remove paragr -> replace"  "," " -> toUni16 ->
	 * readIn
	 */

	@FXML
	private TextArea txtBox;
	@FXML
	private TextField fieldWords;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private TextField fieldAuthor;
	@FXML
	private TextField fieldTitle;
	@FXML
	private Label progressBarLabel;
	@FXML
	private CheckBox isLatexCode;
	@FXML
	private TextField fieldUrl;
	@FXML
	private TextField fieldMinWords;
	@FXML
	private RadioButton domain1;
	@FXML
	private RadioButton domain2;
	@FXML
	private RadioButton domain3;
	@FXML
	private TextField fieldGetPdfs;
	@FXML
	private TextField FieldReplFirst;
	@FXML
	private TextField FieldReplSec;

	// params (default values):
	int minWordsPerSentence = 0;
	boolean completeBrackets = true;
	boolean cleanUpText = true;
	boolean addParagraphs = true;
	int paragraphNumber = 12;
	boolean toUni2016gh = true;
	boolean removeParagraphs = true;
	boolean removeDoubleWhitespaces = true;

	// Folder for saving TXT files
	String currentFolder = "user.home";

	static int CONFIRMATIONHELPER = 0;// 1==yes, 2==cancel

	//Keyboard Shortcuts
	@FXML
	public void keyInput(KeyEvent key) {
		String code = String.valueOf(key.getCode());

		switch (code) {
		case "S":
			saveTxtFile();
			break;
		case "W":
			createText("createText");
			break;
		case "E":
			createText("createPdf");
			break;
		default:
			// do nothing
		}
		key.consume();
	}

	@FXML
	protected void pdfCreationAct(ActionEvent event) throws Exception {
		createText("createPdf");
	}

	public void makePdf() {
		showProgressBarText("Creating Pdf...");
		addToLog("Creating a PDF file.");
		long tStart = System.currentTimeMillis();

		// get text (which was just created)
		String booktext = txtBox.getText();

		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta / 1000.0;
		addToLog(" Create pdf...");

		try {
			makeNewPdf(booktext, fieldAuthor.getText(), fieldTitle.getText(), elapsedSeconds);
		} catch (DocumentException | IOException e) {
			e.printStackTrace();
			addToLog("Error: " + e.getLocalizedMessage());
			DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
		}
		addToLog("default" + " Pdf created!");

		progressBar.setVisible(false);
		hideProgressBarText();
	}

	@FXML
	protected void txtCreationAct(ActionEvent event) throws Exception {
		createText("createText");
	}

	public void createText(String currentTask) {
		Thread thread = new Thread() {
			public void run() {

				Double progressValue = 0.05;
				progressBar.setVisible(true);
				showProgressBarText("Creating text...");
				setProgress(progressValue);

				long tStart = System.currentTimeMillis();

				File folder = new File("Vocabulary");
				File[] listOfFiles = folder.listFiles();

				boolean containsTxt = false;
				for (File file : listOfFiles)
					if (file.toString().endsWith(".txt"))
						containsTxt = true;

				double elapsedSeconds = 0.0;

				if (containsTxt) {
					// Get file for first word
					ArrayList<String> wordList = getFirstWord(listOfFiles);

					String randomWord = "";
					if (wordList.size() > 1)
						randomWord = wordList.get((ThreadLocalRandom.current().nextInt(0, wordList.size() - 1)));
					else
						randomWord = wordList.get(0);
					String booktext = randomWord;

					// add varianz
					int wordCount = Integer.parseInt(fieldWords.getText());

					// 0.95 progress muss aufgefüllt werden.
					// wordcount zB 10, 1000, 1000000
					// pro Prozent:0,1, 10, 10000
					double wordsPerProgress = wordCount / 95;

					// create a latex code (which can be copy-pasted into e.g. Overleaf)
					if (isLatexCode.isSelected())
						booktext = createLatexCode(wordList, randomWord, booktext, progressValue, wordsPerProgress,
								wordCount);
					else {
						// try to create longer sentences
						if (minWordsPerSentence > 0)
							booktext = createTextWithMinWords(wordList, randomWord, booktext, progressValue,
									wordsPerProgress, wordCount);
						// default case - create normal text
						else
							booktext = createTextQuickly(wordList, randomWord, booktext, progressValue,
									wordsPerProgress, wordCount);
					}

					txtBox.setText(booktext);

					// save text persistently:
					Writer writer;
					try {
						writer = new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream("Magazin/txtBoxTxtPersistent"), "utf-8"));
						writer.write(booktext);
						writer.close();
					} catch (Exception e) {
						DialogBoxes.showErrorBox("Error!", "Could not save TextBox text: \n" + e.getMessage(),
								e.getLocalizedMessage());
						e.printStackTrace();
					}

					long tEnd = System.currentTimeMillis();
					long tDelta = tEnd - tStart;
					elapsedSeconds = tDelta / 1000.0;

					setProgress(1);
					// final int tempwordCount = wordCount;

				} else {
					DialogBoxes.showErrorBox("No Vocabulary!", "No Vocabulary files were detected!",
							"You need to read a Vocabulary in before you can write a text!");
				}
				progressBar.setVisible(false);
				hideProgressBarText();

				// next Task: convert to normal text
				uniTextConversion(currentTask, elapsedSeconds);
			}
		};
		thread.start();
	}

	public String createTextWithMinWords(ArrayList<String> wordList, String randomWord, String booktext,
			double progressValue, double wordsPerProgress, int wordCount) {
		int wordsInCurrentSentence = 0;
		for (int w = 0; w < wordCount; w++) {
			if (w % wordsPerProgress == 0) {
				showProgressBarText("Creating text... (" + (int) ((progressValue * 100) - 4) + "%)");
				progressValue = progressValue + 0.01;
				setProgress(progressValue - 0.04);
			}
			try {

				int whilehelper = 0;
				// get a random word
				wordList = getArraylistFromFile("Vocabulary/" + randomWord + ".txt");
				randomWord = wordList.get((ThreadLocalRandom.current().nextInt(0, wordList.size())));

				// if min wordsPerSentence weren't reached yet, search for words to make the sentence longer
				while (minWordsPerSentence > wordsInCurrentSentence && randomWord.equals("U46")) {
					randomWord = wordList.get((ThreadLocalRandom.current().nextInt(0, wordList.size())));

					// if only periods are available, break the loop:
					whilehelper++;
					if (whilehelper > 100)
						break;
				}
				whilehelper = 0;
				if (!randomWord.equals("U46"))
					wordsInCurrentSentence++;
				else // if a sentence was finished (randomWord is a period)
					wordsInCurrentSentence = 0;

				// add word to text
				booktext = booktext + " " + randomWord;

			} catch (Exception e) {
				DialogBoxes.showMessageBox("Error: " + e.getMessage(),
						"For more information, take a look at the log file. \n", "The word: " + randomWord);
				addToLog("Error: Problem with the word \"" + randomWord + "\" - " + e.getLocalizedMessage()
						+ "\n\nYou may want to convert the vocabulary to the Uni2016gh format if you haven't done so already.");
				e.printStackTrace();
				return booktext;
			}
		}
		return booktext;
	}

	// is faster because some checks are removed:
	public String createTextQuickly(ArrayList<String> wordList, String randomWord, String booktext,
			double progressValue, double wordsPerProgress, int wordCount) {

		for (int w = 0; w < wordCount; w++) {
			if (w % wordsPerProgress == 0) {
				showProgressBarText("Creating text... (" + (int) ((progressValue * 100) - 4) + "%)");
				progressValue = progressValue + 0.01;
				// progressBar.setProgress(progressValue);
				setProgress(progressValue - 0.04);
			}
			try {
				// get a random word
				wordList = getArraylistFromFile("Vocabulary/" + randomWord + ".txt");
				randomWord = wordList.get((ThreadLocalRandom.current().nextInt(0, wordList.size())));

				// add word to text
				booktext = booktext + " " + randomWord;

			} catch (Exception e) {
				DialogBoxes.showMessageBox("Error: " + e.getMessage(),
						"For more information, take a look at the log file. \n", "The word: " + randomWord);
				addToLog("Error: Problem with the word \"" + randomWord + "\" - " + e.getLocalizedMessage()
						+ "\n\nYou may want to convert the vocabulary to the Uni2016gh format if you haven't done so already.");
				e.printStackTrace();
				return booktext;
			}
		}
		return booktext;
	}

	public String getTextOfTxt(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String everything = "";
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			everything = sb.toString();
		} finally {
			br.close();
		}
		return everything;
	}

	@FXML
	public void textReadInProcess() {
		Thread thread = new Thread() {
			public void run() {
				cleanUpTextbox("textReadInProcess");
			}
		};
		thread.start();
	}

	public void readTxtIn() {
		
		boolean dirHasNonDirFiles = false;
		File dir = new File("Vocabulary");
		for (File file : dir.listFiles()) {
			if (!file.isDirectory()) {
				dirHasNonDirFiles = true;
				break;
			}
		}
		if (dirHasNonDirFiles == true) {
			DialogBoxes.showMessageBox("Could not create vocabulary",
					"There is already a vocabulay loaded. \nDelete the vocabulary in the \"" + dir.getAbsolutePath()
							+ "\" folder first.",
					"Vocabulary -> delete voc");
		} else {
			Double progressValue = 0.05;
			progressBar.setVisible(true);
			showProgressBarText("Reading text in...");
			setProgress(progressValue);

			addToLog("Reading into vocabulary folder.");

			String filename = "filename";
			// damit jedes sonderzeichen ein eigenes Wort im Vocab ist
			String txtData = txtBox.getText().replace(",", " , ").replace(".", " . ").replace(";", " ; ")
					.replace(":", " : ").replace("?", " ? ").replace(")", " ) ").replace("(", " ( ").replace("[", " [ ")
					.replace("]", " ] ").replace("!", " ! ").replace("\n", " ").replace("*", " * ")
					.replace("\"", " \" ").replace("<", " < ").replace(">", " > ").replace("/", " / ")
					.replace("\\", " \\ ").replace("|", " | ").replace("\0", " \0 ").replace("„", " „ ")
					.replace("“", " “ ").replace("—", " - ");

			txtData = txtData.replaceAll("  ", " ");
			String[] txtBoxTextString = txtData.split(" ");
			
			// in ReadIn.txt file speichern
			writeToReadIn(txtData);
			txtData = "";

			double wordsPerProgress = txtBoxTextString.length / 95;
			for (int g = 0; g < txtBoxTextString.length; g++) {
				if (g % wordsPerProgress == 0) {
					progressValue = progressValue + 0.01;
					setProgress(progressValue);
				}
				filename = txtBoxTextString[g];
				if (!fileExistsinVocab(filename, "Vocabulary"))
					addWordToVocab(txtBoxTextString, filename, "Vocabulary");
			}

			// get number of files in folder
			int wordInVocabNum = 0;
			for (File file : dir.listFiles())
				if (!file.isDirectory())
					wordInVocabNum++;

			DialogBoxes.showMessageBox("Vocabulary created!", "Reading in finished!",
					"All " + wordInVocabNum + " words are now in the 'Vocabulary' folder.");
			progressBar.setVisible(false);
			hideProgressBarText();
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					txtBox.setText("");
				}
			});
		}
	}

	public void addWordToVocab(String[] txtBoxTextString, String filename, String path) {
		List<String> lines = new ArrayList<String>();
		for (int i = 0; i < txtBoxTextString.length; i++) {
			// also check if value exists
			if (txtBoxTextString[i].equals(filename) && txtBoxTextString.length > i + 1
					&& txtBoxTextString[i + 1] != null)
				lines.add(txtBoxTextString[i + 1]);
		}

		// letztes wort führt zu erstem Wort
		if (txtBoxTextString[txtBoxTextString.length - 1].equals(filename))
			lines.add(txtBoxTextString[0]);

		Path file = null;
		try {
			file = Paths.get(path + "/" + filename + ".txt");
		} catch (InvalidPathException e) {
			e.printStackTrace();
			addToLog("Error: " + e.getLocalizedMessage() + "\nFile name '" + file + "' is not allowed!");
			DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
		}

		try {
			Files.write(file, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Could not create File!", "IOException",
					"Something went wrong :/ \n" + e.getMessage());
			addToLog("Error: " + e.getLocalizedMessage());
		}
	}

	public static boolean showChoiceBox(String Title, String HeaderText, String ContextText) {
		// int CONFIRMATIONHELPER=0;//1==yes, 2==cancel
		CONFIRMATIONHELPER = 0;

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle(Title);
				alert.setHeaderText(HeaderText);
				alert.setContentText(ContextText);
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK) {
					CONFIRMATIONHELPER = 1;
				} else {
					CONFIRMATIONHELPER = 2;
				}
			}
		});
		while (CONFIRMATIONHELPER == 0)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
			}
		if (CONFIRMATIONHELPER == 1) {
			CONFIRMATIONHELPER = 0;
			return true;
		} else {
			CONFIRMATIONHELPER = 0;
			return false;
		}
	}

	public void showProgressBarText(String Text) {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				progressBarLabel.setVisible(true);
				progressBarLabel.setText(Text);
			}
		});
	}

	public void hideProgressBarText() {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				progressBarLabel.setVisible(false);
			}
		});
	}

	@FXML
	public void getPdfText() {
		Thread thread = new Thread() {
			public void run() {
				String errorFileName = "";
				try {
					Double progressValue = 0.05;
					progressBar.setVisible(true);
					showProgressBarText("Getting Pdf text...");
					setProgress(progressValue);

					File folder = new File("Magazin/Book");
					File[] listOfFiles = folder.listFiles();

					String st = "";
					double wordsPerProgress = listOfFiles.length / 95;
					int turnNumber = 0;

					for (File file : listOfFiles) {
						errorFileName = String.valueOf(file);
						turnNumber++;
						if (turnNumber % wordsPerProgress == 0) {
							progressValue = progressValue + 0.01;
							setProgress(progressValue);
						}
						if (file.toString().endsWith(".pdf")) {

							PDDocument document = null;
							document = PDDocument.load(file);
							document.getClass();
							// if (!document.isEncrypted()) {
							PDFTextStripperByArea stripper = new PDFTextStripperByArea();
							stripper.setSortByPosition(true);
							PDFTextStripper Tstripper = new PDFTextStripper();

							st = st + Tstripper.getText(document);
							// }
							document.close();
						} else if (file.toString().endsWith(".txt")) {
							StringBuilder content = new StringBuilder();
							try (FileReader fileStream = new FileReader(file);
									BufferedReader bufferedReader = new BufferedReader(fileStream)) {

								String line = null;
								while ((line = bufferedReader.readLine()) != null) {
									content.append(line + "\n");
								}
								String contentString = content.toString();
								contentString = contentString.substring(0, contentString.length() - 1);

								st = st + contentString;
							} catch (FileNotFoundException ex) {
								ex.printStackTrace();
								DialogBoxes.showErrorBox("Error!", ex.getMessage(), ex.getLocalizedMessage());
							} catch (IOException ex) {
								ex.printStackTrace();
								DialogBoxes.showErrorBox("Error!", ex.getMessage(), ex.getLocalizedMessage());
							}
						}
						st = st + "\n\n\n\n\n";
						// Abstand zwischen einzelnen pdf file outputs
					}
					// list of files read in as String
					String readInFileString = "";
					for (File endFile : listOfFiles) {
						readInFileString = readInFileString + "\n" + endFile;
					}
					DialogBoxes.showMessageBox("Reading in successful!",
							"The following books were read into the TextBox: ", readInFileString);

					txtBox.setText(st);
					progressBar.setVisible(false);
					hideProgressBarText();
				} catch (Exception e) {
					e.printStackTrace();
					DialogBoxes.showErrorBox("Error", "Error: " + errorFileName + "\n" + e.getMessage(),
							e.getLocalizedMessage());
					addToLog("Error: " + e.getLocalizedMessage());
				}
			}
		};
		thread.start();
	}

	public void cleanUpTextbox(String currentTask) {

		// button pressed OR param true (default)
		if (currentTask.equals("") || cleanUpText == true) {

			Double progressValue = 0.05;
			progressBar.setVisible(true);
			showProgressBarText("Cleaning up TextBox...");
			setProgress(progressValue);

			try {
				String st = txtBox.getText();
				st = st.replace("\n", " ");
				st = st.replace(".", ".\n");

				st = st.replaceAll("  ", " ");
				st = st.replace("\n ", "\n");
				// find 'fake sentences'
				st = st.replace(". \na", ". a").replace(". \nb", ". b").replace(". \nc", ". c").replace(". \nd", ". d")
						.replace(". \ne", ". e").replace(". \nf", ". f").replace(". \ng", ". g").replace(". \nh", ". h")
						.replace(". \ni", ". i").replace(". \nj", ". j").replace(". \nk", ". k").replace(". \nl", ". l")
						.replace(". \nm", ". m").replace(". \nn", ". n").replace(". \no", ". o").replace(". \np", ". p")
						.replace(". \nq", ". q").replace(". \nr", ". r").replace(". \ns", ". s").replace(". \nt", ". t")
						.replace(". \nu", ". u").replace(". \nv", ". v").replace(". \nw", ". w").replace(". \nx", ". x")
						.replace(". \ny", ". y").replace(". \nz", ". z");

				String[] sentences = st.split("\n");
				String finishedString = "";
				int turnNumber = 0;
				double wordsPerProgress = sentences.length / 95;
				for (String sentence : sentences) {
					turnNumber++;
					if (turnNumber % wordsPerProgress == 0) {
						progressValue = progressValue + 0.01;
						setProgress(progressValue);
					}
					// no sentences with few words
					String[] words = sentence.split(" ");
					if (words.length < 5)
						sentence = "";
					else {
						// only special chars
						if (sentence.matches("[-/@#$%^&_+=()]+"))
							sentence = "";
						// keine komisch beginnenden Sätze
						if (!Character.isUpperCase(sentence.charAt(0)) && !Character.isDigit(sentence.charAt(0)))
							sentence = "";
						// sentence doesnt end with '.' -> no legitimate
						// sentence
						if (!sentence.endsWith(".") && !sentence.endsWith(". "))
							sentence = "";
						// no short sentences
						if (sentence.length() < 25)
							sentence = "";
						// dont start with a whitespace
						while (sentence.startsWith(" "))
							sentence = sentence.substring(1);

						// check how many digits there are
						// >10 digits OR many (25%+) digits in sentence->
						// remove!
						double digitCounter = 0;
						double letterCounter = 0;
						for (char c : sentence.toCharArray()) {
							if (c >= '0' && c <= '9') {
								++digitCounter;
							} else
								++letterCounter;
						}
						if (digitCounter > 9 || (letterCounter / digitCounter) <= 4 || sentence.contains("©"))
							sentence = "";

						finishedString = finishedString + sentence + "\n";
					}
				}
				// get rid of empty lines
				while (finishedString.startsWith("\n"))
					finishedString = finishedString.substring(1);
				finishedString = finishedString.replaceAll("\n\n", "\n");
				finishedString = finishedString.replaceAll("  ", " ");
				finishedString = finishedString.replaceAll("- ", "");

				txtBox.setText(finishedString);

			} catch (Exception e) {
				e.printStackTrace();
				addToLog("Error: " + e.getLocalizedMessage());
				DialogBoxes.showErrorBox("Error", e.getMessage(), e.getLocalizedMessage());
			}
			progressBar.setVisible(false);
			hideProgressBarText();
		}

		if (currentTask.equals("textReadInProcess"))
			removeParagraphs(currentTask);
	}

	public boolean fileExistsinVocab(String filename, String path) {
		File file = new File(path + "/" + filename + ".txt");
		if (file.isFile() && file.exists() && !file.isDirectory()) {
			return true;
		} else
			return false;
	}

	private ArrayList<String> getArraylistFromFile(String filePath) throws FileNotFoundException {
		Scanner s = new Scanner(new File(filePath));
		ArrayList<String> list = new ArrayList<String>();
		while (s.hasNext()) {
			list.add(s.next());
		}
		s.close();

		return list;
	}

	public void makeNewPdf(String text, String author, String title, double elapsedSeconds)
			throws MalformedURLException, DocumentException, IOException {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					if (text.length() < 2)
						DialogBoxes.showErrorBox("Error!", "Could not read text. Please try again.", "");
					else
						textCreation.PdfCreationService.createAPdf(title, author, text, elapsedSeconds);

				} catch (DocumentException | IOException e) {
					e.printStackTrace();
					addToLog("Error: " + e.getLocalizedMessage());
					DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
				}
			}
		});
	}

	@FXML
	public void openReadme() {
		try {
			String ReadMeTxt = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/ReadMe")));
			txtBox.setText(ReadMeTxt);
		} catch (IOException e) {
			e.printStackTrace();
			addToLog("Error: " + e.getLocalizedMessage());
			DialogBoxes.showErrorBox("Error!", "Could not read ReadMe file: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	@FXML
	public void deleteVocab() {
		Thread thread = new Thread() {
			public void run() {
				Double progressValue = 0.05;
				progressBar.setVisible(true);
				showProgressBarText("Deleting vocabulary...");
				setProgress(progressValue);

				String vocPath = "Vocabulary";
				boolean delVocab = showChoiceBox("Delete Vocabulary", "Delete all vocabulary files (" + vocPath + ")?",
						"This will delete the .txt files.");

				if (delVocab) {
					addToLog("Deleting " + vocPath + " files.");
					int num = 0;
					File dir = new File(vocPath);
					int turnNumber = 0;
					double wordsPerProgress = dir.listFiles().length / 95;
					for (File file : dir.listFiles()) {
						turnNumber++;
						if (!file.isDirectory()) {
							if (turnNumber % wordsPerProgress == 0) {
								progressValue = progressValue + 0.01;
								setProgress(progressValue);
							}
							file.delete();
							num++;
						}
					}
					progressBar.setVisible(false);
					hideProgressBarText();
					if (num == 0)
						DialogBoxes.showMessageBox("Empty Vocabulary!",
								" The vocabulary is already empty! (" + num + ")", "");
					else
						DialogBoxes.showMessageBox("Vocabulary deleted!", num + " files were deleted", "");
				}
			}
		};
		thread.start();
	}

	@FXML
	public void removeSpecialSigns() { // (){}[]"„
		
		String textBoxTxt = txtBox.getText();
		textBoxTxt = textBoxTxt.replace("(", "").replace(")", "").replace("[", "").replace("]", "").replace("{", "")
				.replace("}", "").replace("\"", "").replace("„", "").replace("″", "");
		textBoxTxt = textBoxTxt.replaceAll("  ", " ");
		txtBox.setText(textBoxTxt);
	}

	// The same function as below, but for the UI
	@FXML
	public void addParagraphsThread() {
		Thread thread = new Thread() {
			public void run() {
				addParagraphs("");
			}
		};
		thread.start();
	}

	// Add paragraphs in random places
	public void addParagraphs(String currentTask) {

		// if button was pressed OR bool param is true (default)
		if (currentTask.equals("") || addParagraphs == true) {

			Double progressValue = 0.05;
			progressBar.setVisible(true);
			showProgressBarText("Adding Paragraphs");
			setProgress(progressValue);

			String textBoxTxt = txtBox.getText();

			String[] txtBoxTextString = textBoxTxt.replace("...", "23156473856").replace("..", ".").split("\\. ");
			int randomNumber = 0;
			int longPara = 0;
			int shortPara = 0;
			textBoxTxt = "";

			int turnNumber = 0;
			double wordsPerProgress = txtBoxTextString.length / 95;
			for (String sentence : txtBoxTextString) {
				turnNumber++;
				if (turnNumber % wordsPerProgress == 0) {
					progressValue = progressValue + 0.01;
					setProgress(progressValue);
				}
				randomNumber = (ThreadLocalRandom.current().nextInt(0, 15 + 1));

				// paragraphNumber param - default=12. lower val - more line breaks.
				if (randomNumber > paragraphNumber && randomNumber != 15) {
					sentence = sentence + ". \n";
					shortPara++;
				} else if (randomNumber == 15) {
					sentence = sentence + ". \n\n";
					longPara++;
				} else
					sentence = sentence + ". ";

				textBoxTxt = textBoxTxt + sentence;

				// remove period on end
				if (textBoxTxt.endsWith(".. "))
					textBoxTxt = textBoxTxt.substring(0, textBoxTxt.length() - 2);
			}

			textBoxTxt = textBoxTxt.replace("23156473856", "...");
			txtBox.setText(textBoxTxt);
			if (currentTask.equals(""))
				DialogBoxes.showMessageBox("Added Paragraphs!", (longPara + shortPara) + " paragraphs were added.",
						longPara + " long paragraphs\n" + shortPara + " short paragraphs");
			progressBar.setVisible(false);
			hideProgressBarText();
		}

		if (currentTask.equals("createText") || currentTask.equals("createPdf"))
			lastTextFixes(currentTask);
	}

	public void lastTextFixes(String currentTask) {
		String currentText = txtBox.getText();

		// escape symbols for Latex code
		if (isLatexCode.isSelected())
			currentText = currentText.replace("<br>", "\n").replace("<spacesign>", " ").replace("%", "\\%")
					.replace("\\%Main part", "%Main part").replace("^", "\\^").replace("$", "\\$").replace("∨", "\\∨")
					.replace("_", "\\_").replace("\\<", "<") // Yes, you need to do it the other way around for these
																// signs.
					.replace("\\>", ">");

		txtBox.setText(currentText);

		// save newly created text persistently. It will be opened next time.
		saveTxtboxText();

		// if it's the text creation process, continue to the last step
		if (currentTask.equals("createPdf"))
			makePdf();
	}

	@FXML
	public void checkVocab() {
		Thread thread = new Thread() {
			public void run() {

				Double progressValue = 0.05;
				progressBar.setVisible(true);
				showProgressBarText("Evaluating vocabulary...");
				setProgress(progressValue);

				// seconds per word (totalVoc):
				// 0,00133200643037587078006583480058

				long tStart = System.currentTimeMillis();

				DecimalFormat df = new DecimalFormat("#.#####");
				df.setRoundingMode(RoundingMode.CEILING);

				File folder = new File("Vocabulary");
				File[] listOfFiles = folder.listFiles();

				List<Integer> wordsConnections = new ArrayList<Integer>();

				double avgConnections;
				int totalConnections = 0;
				int maxConnections = 0;
				int minConnections = 0;
				String MaxConnected = "";
				String MinConnected = "";
				int iterationNum = 0;
				int currentSize = 0;

				// int FcurrentSize = 0;
				int FtotalConnections = 0;
				// int FiterationNum = 0;
				int turnNumber = 0;
				double wordsPerProgress = listOfFiles.length / (95 / 3);
				for (File file : listOfFiles) {
					turnNumber++;
					if (turnNumber % wordsPerProgress == 0) {
						progressValue = progressValue + 0.01;
						setProgress(progressValue);
					}
					if (!file.isDirectory() && file.toString().endsWith(".txt")) {
						@SuppressWarnings("unused")
						ArrayList<String> wordList = null;
						try {
							wordList = getArraylistFromFile(file.toString());
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							addToLog("Error: " + e.getLocalizedMessage());
							DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
						}
					}
				}
				boolean checkVocab = true;
				int waitingTime = (int) (FtotalConnections * 0.00133200643037587078006583480058);
				if (waitingTime > 5)
					checkVocab = showChoiceBox("Create Statistic?", "",
							"This will take about " + waitingTime + " seconds.");

				if (checkVocab) {
					turnNumber = 0;
					for (File file : listOfFiles) {
						turnNumber++;
						wordsPerProgress = listOfFiles.length / (95 / 3);
						if (turnNumber % wordsPerProgress == 0) {
							progressValue = progressValue + 0.01;
							setProgress(progressValue);
						}
						if (!file.isDirectory() && file.toString().endsWith(".txt")) {
							try {
								ArrayList<String> wordList = getArraylistFromFile(file.toString());
								currentSize = wordList.size();
								wordsConnections.add(currentSize);
								totalConnections = totalConnections + currentSize;
								if (iterationNum != 0)
									if (currentSize > maxConnections) {
										maxConnections = currentSize;
										MaxConnected = file.toString();
									} else if (currentSize < minConnections) {
										minConnections = currentSize;
										MinConnected = file.toString();
									} else {
									}
								else {
									maxConnections = currentSize;
									minConnections = currentSize;
									MaxConnected = file.toString();
									MinConnected = file.toString();
								}

							} catch (FileNotFoundException e) {
								e.printStackTrace();
								addToLog("Error: " + e.getLocalizedMessage());
								DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
								break;
							}
							iterationNum++;
						}
					}
					avgConnections = ((double) totalConnections) / (((double) iterationNum));
					int errorNum = 0;
					String errorStr = "";
					@SuppressWarnings("unused")
					int iteratorNum = 0;
					turnNumber = 0;
					for (File file : listOfFiles) {
						turnNumber++;
						wordsPerProgress = listOfFiles.length / (95 / 3);
						if (turnNumber % wordsPerProgress == 0) {
							progressValue = progressValue + 0.01;
							setProgress(progressValue);
						}
						iteratorNum++;

						if (!file.isDirectory() && file.toString().endsWith(".txt")) {
							try {
								ArrayList<String> wordList = getArraylistFromFile(file.toString());
								for (String word : wordList) {
									try {
										wordList = getArraylistFromFile("Vocabulary/" + word + ".txt");
										//String testWord = wordList.get((ThreadLocalRandom.current().nextInt(0, wordList.size())));
									} catch (Exception e) {
										errorNum++;
										errorStr = errorStr + file.toString() + "  :  " + word + "\n";
									}
								}
							} catch (Exception e) {
								errorNum++;
							}
						}
					}

					long tEnd = System.currentTimeMillis();
					long tDelta = tEnd - tStart;
					double elapsedSeconds = tDelta / 1000.0;
					double errorprob = (((double) errorNum) / ((double) totalConnections));
					double firstExpError = 0;
					if (errorprob != 0)
						firstExpError = 1 / errorprob;

					// in %
					errorprob = errorprob * 100;

					double vocabQuality = iterationNum / 10;

					if (firstExpError == 0)
						vocabQuality = vocabQuality * 2;
					else {
						if ((avgConnections * firstExpError) < vocabQuality)
							vocabQuality = vocabQuality + avgConnections * firstExpError;
						else
							vocabQuality = vocabQuality + 0.9 * vocabQuality;
					}

					if (iterationNum == 0)
						DialogBoxes.showMessageBox("Could not create vocabulary statistics",
								"There are no words in the vocabulary!", "Read a vocabulary in and try again.");
					else {
						String solutionString = "Vocabulary statistics\n\nStatistic creation time: "
								+ df.format(elapsedSeconds) + " seconds\nWords: " + iterationNum
								+ "\nTotal connections: " + totalConnections + "\nAverage connections: "
								+ df.format(avgConnections) + "\nMin connections: " + MinConnected + "("
								+ minConnections + ")\nMax connections: " + MaxConnected + "(" + maxConnections
								+ ")\n\nErrors: " + errorNum + "\nError probability: " + df.format(errorprob)
								+ "%\nFirst expected error: " + df.format(firstExpError) + "\n\nQuality: "
								+ df.format(vocabQuality) + "\n\n\n" + errorStr;
						DialogBoxes.showMessageBox("Vocabulary statistics",
								"Statistic creation time: " + df.format(elapsedSeconds) + " seconds\nWords: "
										+ iterationNum + "\nTotal connections: " + totalConnections
										+ "\nAverage connections: " + df.format(avgConnections) + "\nMin connections: "
										+ MinConnected + "(" + minConnections + ")\nMax connections: " + MaxConnected
										+ "(" + maxConnections + ")",
								"Errors: " + errorNum + "\nError probability: " + df.format(errorprob)
										+ "%\nFirst expected error: " + df.format(firstExpError) + "\n\nQuality: "
										+ df.format(vocabQuality));
						txtBox.setText(solutionString);
						addToLog(solutionString);
					}
				}
				progressBar.setVisible(false);
				hideProgressBarText();
			}
		};
		thread.start();
	}

	@FXML
	public void copyVocab() {
		Thread thread = new Thread() {
			public void run() {

				Double progressValue = 0.05;
				progressBar.setVisible(true);
				showProgressBarText("Copying vocabulary...");
				setProgress(progressValue);

				// http://www.baeldung.com/java-how-to-rename-or-move-a-file

				boolean checkVocab = showChoiceBox("Copy Vocabulary", "Copy all vocabulary files?",
						"This will copy the .txt files into a new folder.");

				if (checkVocab) {
					String newFolderName = "" + System.currentTimeMillis() + "_"
							+ (ThreadLocalRandom.current().nextInt(0, 999999999 + 1));

					// InputStream inStream = null;
					// OutputStream outStream = null;
					int numberOfFiles = 0;
					// für jedes Wort (File)
					File folder = new File("Vocabulary");
					File[] listOfFiles = folder.listFiles();
					String errorMsg = "";
					new File("Vocabulary/" + newFolderName + "/Vocabulary").mkdirs();
					int turnNumber = 0;
					double wordsPerProgress = listOfFiles.length / 95;
					for (File filename : listOfFiles) {
						turnNumber++;
						if (turnNumber % wordsPerProgress == 0) {
							progressValue = progressValue + 0.01;
							setProgress(progressValue);
						}
						if (!filename.isDirectory())
							try {
								String newFilename = "Vocabulary/" + newFolderName + "/" + filename;

								Path fileToMovePath = (Paths.get(filename + ""));
								Path targetPath = (Paths.get(newFilename));
								Files.move(fileToMovePath, targetPath);

								numberOfFiles++;
							} catch (Exception e) {
								e.printStackTrace();
								addToLog("Error: " + e.getLocalizedMessage());
								errorMsg = e.getLocalizedMessage();
								DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
								break;
							}
					}
					if (numberOfFiles == 0)
						DialogBoxes.showErrorBox("Vocabulary files not copied!", numberOfFiles + " files were copied",
								"Something went wrong: " + errorMsg);
					else
						DialogBoxes.showMessageBox("Vocabulary files copied!",
								numberOfFiles + " files were copied successfully", "Check them out in the new "
										+ newFolderName + " folder. \nRename the folder accordingly.");
				}
				progressBar.setVisible(false);
				hideProgressBarText();
			}
		};
		thread.start();
	}

	@FXML
	public void randtxtCreationAct() {
		Thread thread = new Thread() {
			public void run() {

				int z = Integer.parseInt(fieldWords.getText());

				txtBox.setText(makeRandomText(z));
				progressBar.setVisible(false);
				hideProgressBarText();
			}
		};
		thread.start();
	}

	private String makeRandomText(int zeichen) {

		char[] Letters = { '.', ',', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
				'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', ' ', ' ', ' ', ' ', ' ', 'a', 'e', 'i', 'o', 'u', 'a',
				'e', 'u', 'o', 'i' };
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (; zeichen > 0; zeichen--) {
			char randChar = Letters[random.nextInt(Letters.length)];
			sb.append(randChar);

			// Absatz
			if (randChar == ('.')) {
				if (1 == (ThreadLocalRandom.current().nextInt(1, 8 + 1)))
					sb.append('\n');
			}
		}
		String randomText = "";
		String randomTextTemp = sb.toString();
		String[] randomTextWords = randomTextTemp.split(" ");
		boolean isBigWord = true;
		for (int i = 0; i < randomTextWords.length - 1; i++) {
			if (1 == (ThreadLocalRandom.current().nextInt(1, 3 + 1)) || isBigWord) {
				// (ThreadLocalRandom.current().nextInt(min, max + 1))
				randomTextWords[i] = firstLetterUppercase(randomTextWords[i]);
				isBigWord = false;
			}
			randomText = randomText + randomTextWords[i] + " ";
			if (randomTextWords[i].contains("."))
				isBigWord = true;
		}
		randomText = randomText + ".";
		randomText = randomText.replace("..", ".");
		randomText = randomText.replace(",,", ",");
		randomText = randomText.replace(" .", ".");
		randomText = randomText.replace(" ,", ",");
		randomText = randomText.replace(",", ", ");
		randomText = randomText.replace(".", ". ");
		randomText = randomText.replaceAll(",", ", ");
		randomText = randomText.replaceAll("  ", " ");

		// check if title is just a .
		if (randomText.equals("."))
			randomText = makeRandomText(zeichen);

		return randomText;
	}

	private String firstLetterUppercase(String word) {
		if (word.length() > 2)
			word = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
		return word;
	}

	@FXML
	public void binaryTextConversion() {
		Thread thread = new Thread() {
			public void run() {

				Double progressValue = 0.05;
				progressBar.setVisible(true);
				setProgress(progressValue);

				try {
					String s = txtBox.getText();

					// binary -> text
					if (s.matches("[0-1 ]+")) {
						showProgressBarText("Converting to text...");
						String str = "";
						String[] chars = s.split(" ");
						int charCode = 0;
						int turnNumber = 0;
						double wordsPerProgress = chars.length / 95;
						for (String character : chars) {
							turnNumber++;
							if (turnNumber % wordsPerProgress == 0) {
								progressValue = progressValue + 0.01;
								setProgress(progressValue);
							}
							charCode = Integer.parseInt(character, 2);
							str = str + Character.valueOf((char) charCode).toString();
						}

						txtBox.setText(str);

						// text -> binary
					} else {
						showProgressBarText("Converting to binary...");
						byte[] bytes = s.getBytes();
						StringBuilder binary = new StringBuilder();
						int turnNumber = 0;
						double wordsPerProgress = bytes.length / 95;
						for (byte by : bytes) {
							if (turnNumber % wordsPerProgress == 0) {
								progressValue = progressValue + 0.01;
								setProgress(progressValue);
							}
							int val = by;
							for (int i = 0; i < 8; i++) {
								binary.append((val & 128) == 0 ? 0 : 1);
								val <<= 1;
							}
							binary.append(' ');
						}
						String str = (String.valueOf(binary));

						txtBox.setText(str);
					}
				} catch (Exception e) {
					e.printStackTrace();
					addToLog("Error: " + e.getLocalizedMessage());
					DialogBoxes.showErrorBox("Error detected!", e.getMessage(), e.getLocalizedMessage());
				}
				progressBar.setVisible(false);
				hideProgressBarText();
			}
		};
		thread.start();
	}

	@FXML
	public void uniTextConversionThread() {
		Thread thread = new Thread() {
			public void run() {
				uniTextConversion("", 0.0);
			}
		};
		thread.start();
	}

	public void uniTextConversion(String currentTask, double elapsedSeconds) {

		// if the button was pressed OR bool is true in params (default)
		if (currentTask.equals("") || toUni2016gh == true) {
			Double progressValue = 0.05;
			progressBar.setVisible(true);
			setProgress(progressValue);

			DecimalFormat df = new DecimalFormat("#.##");
			df.setRoundingMode(RoundingMode.CEILING);

			try {
				String s = txtBox.getText();
				String str = "";

				// to text
				if (s.matches("[0-9U \n]+")) {
					showProgressBarText("Converting to text...");
					String[] words = s.split(" ");
					int turnNumber = 0;
					double wordsPerProgress = words.length / 95;
					for (String word : words) {
						turnNumber++;
						if (turnNumber % wordsPerProgress == 0) {
							showProgressBarText("Converting from Uni2016gh... (" + (int) (progressValue * 100) + "%)");
							progressValue = progressValue + 0.01;
							setProgress(progressValue);
						}
						String[] wordSigns = word.split("U");
						for (String wordSign : wordSigns) {
							if (wordSign != null && !wordSign.isEmpty()) {
								int char1 = Integer.valueOf(wordSign);
								str = str + (char) char1;
							}
						}
					}
					// if booktext starts with " ", remove whitespace
					str = str.startsWith(" ") ? str.substring(1) : str;
					txtBox.setText(str);
				} else {// to uni
					showProgressBarText("Converting to Uni2016gh...");
					// space: U32
					// enter: U10
					int turnNumber = 0;
					double wordsPerProgress = s.length() / 95;
					for (int i = 0; i < s.length(); i++) {
						turnNumber++;
						if (turnNumber % wordsPerProgress == 0) {
							showProgressBarText("Converting to Uni2016gh... (" + (int) (progressValue * 100) + "%)");
							progressValue = progressValue + 0.01;
							setProgress(progressValue);
						}
						int char1 = s.charAt(i);
						if (char1 == 32 || char1 == 10)
							str = str + "U" + " ";
						str = str + "U" + char1;
					}
					str = str.replace("U46", " U46 ");
					str = str.replaceAll("  ", " ");

					txtBox.setText(str);
				}

			} catch (Exception e) {
				e.printStackTrace();
				addToLog("Error: " + e.getLocalizedMessage());
				DialogBoxes.showErrorBox("Error detected!", e.getMessage(), e.getLocalizedMessage());
			}
			progressBar.setVisible(false);
			hideProgressBarText();
		}

		if (currentTask.equals("createText") || currentTask.equals("createPdf")) {
			String[] txtBoxTxtArray = txtBox.getText().split(" ");
			int wordsCounted[] = { txtBoxTxtArray.length - 1 };
			if (wordsCounted[0] < 0)
				wordsCounted[0] = 0;

			if (currentTask.equals("createText"))
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						DialogBoxes.showMessageBox("Text created!", wordsCounted[0] + " words inserted.",
								"Creation time: " + elapsedSeconds + " seconds.");
					}
				});

			completeBrackets(currentTask);
		} else if (currentTask.equals("textReadInProcess"))
			readTxtIn();
	}

	@FXML
	public void openLog() {
		try {
			String ReadMeTxt = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/Log")));
			txtBox.setText(ReadMeTxt);
		} catch (IOException e) {
			e.printStackTrace();
			addToLog("Error: " + e.getLocalizedMessage());
			DialogBoxes.showErrorBox("Error!", "Could not open log file: \n" + e.getMessage(), e.getLocalizedMessage());
		}
	}

	@FXML
	public void clearLog() {

		Thread thread = new Thread() {
			public void run() {
				boolean clearVocab = showChoiceBox("Delete Log", "Clear the Log file?",
						"This will remove the log file.");
				if (clearVocab)
					try (Writer writer = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream("Magazin/Log"), "utf-8"))) {
						writer.write("");
					} catch (Exception e) {
						e.printStackTrace();
						addToLog("Error: " + e.getLocalizedMessage());
						DialogBoxes.showErrorBox("Error!", "Could not clear log file: \n" + e.getMessage(),
								e.getLocalizedMessage());
					}
			}
		};
		thread.start();
	}

	public void addToLog(String logAddition) {
		try {
			String LogTxt = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/Log")));

			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Magazin/Log"), "utf-8"));
			writer.write(LogTxt + "\n>" + ((new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss.SSS")).format(new Date())) + ": "
					+ logAddition);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not add to log file: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	@FXML
	public void clearTextBox() {
		txtBox.setText("");
	}

	public void setProgress(double number) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				progressBar.setProgress(number);
			}
		});
	}

	public ArrayList<String> getFirstWord(File[] listOfFiles) {
		// erstes wort holen, zuerst '.', wenns das nicht gibt 'U46', dann '',
		// sonst .txt's durchgehen bis was gefunden wird
		ArrayList<String> wordList = null;
		try {
			wordList = getArraylistFromFile("Vocabulary/..txt");
		} catch (Exception e) {
			try {
				wordList = getArraylistFromFile("Vocabulary/U46.txt");
			} catch (Exception ez) {
				try {
					if (!fileExistsinVocab(".", "Vocabulary"))
						wordList = getArraylistFromFile("Vocabulary/.txt");
				} catch (Exception ex) {
					try {
						for (File firstWordSearch : listOfFiles) {
							wordList = getArraylistFromFile(firstWordSearch + "");
							if ((firstWordSearch + "").endsWith(".txt"))
								break;
						}
					} catch (Exception ext) {
						addToLog("Error: No first word found! \n" + ext.getLocalizedMessage());
					}
				}
			}
		}
		return wordList;
	}

	@FXML
	public void getPdfsInBrowser() {
		String downloadURL = "http://www.freefullpdf.com/#gsc.tab=0&gsc.q=" + fieldGetPdfs.getText() + "&gsc.sort=";
		downloadURL = downloadURL.replace(" ", "%20");
		java.awt.Desktop myNewBrowserDesktop = java.awt.Desktop.getDesktop();
		try {
			java.net.URI myNewLocation = new java.net.URI(downloadURL);
			myNewBrowserDesktop.browse(myNewLocation);
		} catch (Exception e) {
		}
	}

	@FXML
	public void replaceInTxtBox() {
		String newtextToReplace = txtBox.getText().replace(FieldReplFirst.getText(), FieldReplSec.getText());
		txtBox.setText(newtextToReplace);
	}

	@FXML
	public void removeParagraphsThread() {
		removeParagraphs("");
	}

	// remove paragraphs before reading text in
	public void removeParagraphs(String currentTask) {

		if (currentTask.equals("") || removeParagraphs == true) {
			String textToReplace = txtBox.getText();
			String newTextToReplace = textToReplace.replace("\n", " ");
			txtBox.setText(newTextToReplace);
			if ((txtBox.getText()).equals("")) {
				DialogBoxes.showErrorBox("Error!", "Could not remove paragraphs!", "Nothing was changed.");
				txtBox.setText(textToReplace);
			}
		}

		if (currentTask.equals("textReadInProcess"))
			removeDoubleWhitespaces(currentTask);
	}

	// make text cleaner before reading it in
	public void removeDoubleWhitespaces(String currentTask) {

		if (removeDoubleWhitespaces == true) {
			String textToReplace = txtBox.getText();
			while (textToReplace.contains("  "))
				textToReplace = textToReplace.replace("  ", " ");
			txtBox.setText(textToReplace);
		}
		uniTextConversion(currentTask, 0.0);
	}

	@FXML
	public void writeToLog() {
		String inputText = txtBox.getText();
		try {
			String LogTxt = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/Log")));

			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Magazin/Log"), "utf-8"));
			writer.write(LogTxt + "\n>" + ((new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss.SSS")).format(new Date()))
					+ " -Notiz: " + inputText);
			writer.close();
			txtBox.setText("");
		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not add to log file: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	@FXML
	public void openReadIn() {
		try {
			String ReadInTxt = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/ReadIn")));
			txtBox.setText(ReadInTxt);
		} catch (IOException e) {
			e.printStackTrace();
			addToLog("Error: " + e.getLocalizedMessage());
			DialogBoxes.showErrorBox("Error!", "Could not open ReadIn file: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	public void writeToReadIn(String readIn) {
		try {
			// String ReadInTxt = new
			// String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/ReadIn")));

			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Magazin/ReadIn"), "utf-8"));
			writer.write(readIn);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not add to log file: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	public void openVocab() {
		try {
			Desktop.getDesktop().open(new File("Vocabulary"));
		} catch (IOException e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not open the Vocabulary: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	@FXML
	private void initialize() {

		// load username and textbox text from persistent file
		String UserNameStored = "";
		String txtBoxText = "";
		try {
			UserNameStored = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/UserName")));
			if (UserNameStored.equals(""))
				UserNameStored = System.getProperty("user.name");
			txtBoxText = new String(
					java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/txtBoxTxtPersistent")));
		} catch (IOException e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not load author name: " + e.getMessage(),
					e.getLocalizedMessage());
		}
		fieldAuthor.setText(UserNameStored);
		txtBox.setText(txtBoxText);

		loadJsonParams();
	}

	public void loadJsonParams() {
		try {
			JSONObject obj = new JSONObject(
					new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/params.json"))));
			minWordsPerSentence = obj.getJSONObject("params").getInt("minWordsPerSentence");
			completeBrackets = obj.getJSONObject("params").getBoolean("completeBrackets");
			cleanUpText = obj.getJSONObject("params").getBoolean("cleanUpText");
			addParagraphs = obj.getJSONObject("params").getBoolean("addParagraphs");
			paragraphNumber = obj.getJSONObject("params").getInt("paragraphNumber");
			toUni2016gh = obj.getJSONObject("params").getBoolean("toUni2016gh");
			removeParagraphs = obj.getJSONObject("params").getBoolean("removeParagraphs");
			removeDoubleWhitespaces = obj.getJSONObject("params").getBoolean("removeDoubleWhitespaces");
		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
		}
	}

	@FXML
	public void storeUserName() {
		try {
			String UserName = fieldAuthor.getText();
			String UserNameStored = new String(
					java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("Magazin/UserName")));
			if (!UserName.equals(UserNameStored)) {
				Writer writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream("Magazin/UserName"), "utf-8"));
				writer.write(UserName);
				writer.close();
				DialogBoxes.showMessageBox("Username changed!", "Name changed to \"" + UserName + "\"", "");
			}
		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not change UserName file: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	@FXML
	public void showAboutInfo() {
		DialogBoxes.showMessageBox("Info", "Synthekst\n\nVersion 1.0.0",
				"© GerH 2016,2017. Continued and completed 2020.");
	}

	@FXML
	public void goToWebsite() {
		String downloadURL = "http://www.github.com/gh28942";
		java.awt.Desktop myNewBrowserDesktop = java.awt.Desktop.getDesktop();
		try {
			java.net.URI myNewLocation = new java.net.URI(downloadURL);
			myNewBrowserDesktop.browse(myNewLocation);
		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
		}
	}

	@FXML
	public void txtBoxToHex() {
		String txtBoxTxt = txtBox.getText();
		byte[] ba = txtBoxTxt.getBytes();

		StringBuilder str = new StringBuilder();
		for (int i = 0; i < ba.length; i++)
			str.append(String.format("%02x", ba[i]));
		txtBox.setText(str.toString());
	}

	@FXML
	public void txtBoxFromHex() {
		String hex = txtBox.getText();

		StringBuilder str = new StringBuilder();
		for (int i = 0; i < hex.length(); i += 2) {
			str.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
		}
		txtBox.setText(str.toString());
	}

	@FXML
	public void countWords() {
		String txtBoxTxt = txtBox.getText();
		String[] txtBoxTxtArray = txtBoxTxt.split(" ");
		int wordCount = txtBoxTxtArray.length - 1;
		int letterCount = txtBoxTxt.length();
		Matcher m = Pattern.compile("\r\n|\r|\n").matcher(txtBoxTxt);
		int lines = 1;
		while (m.find()) {
			lines++;
		}

		int emptyLines = 0;
		try {
			final BufferedReader br = new BufferedReader(new StringReader(txtBoxTxt));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					emptyLines++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
		}

		if (wordCount < 0)
			wordCount = 0;
		if (letterCount < 0)
			letterCount = 0;
		int letterCountNoWS = txtBoxTxt.replace(" ", "").length();
		DialogBoxes.showMessageBox("Word count", "",
				"Words: " + wordCount + "\nLetters: " + letterCount + "\nLetters (no whitespaces): " + letterCountNoWS
						+ "\n\nLines: " + lines + "\nEmpty lines: " + emptyLines + "\nNon-Empty lines: "
						+ (lines - emptyLines));

	}

	@FXML
	public void toUppercase() {
		String txtBoxTxt = txtBox.getText();
		txtBoxTxt = txtBoxTxt.toUpperCase();
		txtBox.setText(new String(txtBoxTxt));
	}

	@FXML
	public void toLowercase() {
		String txtBoxTxt = txtBox.getText();
		txtBoxTxt = txtBoxTxt.toLowerCase();
		txtBox.setText(new String(txtBoxTxt));
	}

	// The same function as below, but for the UI
	@FXML
	public void completeBracketsThread() {
		Thread thread = new Thread() {
			public void run() {
				completeBrackets("");
			}
		};
		thread.start();
	}

	// complete open brackets in the text
	public void completeBrackets(String currentTask) {
		try {

			// if button pressed OR param=true (default)
			if (currentTask.equals("") || completeBrackets == true) {
				String txtBoxTxt = txtBox.getText();
				txtBox.setText(completeBracketsOfString(txtBoxTxt));
			}

			if (currentTask.equals("createText") || currentTask.equals("createPdf"))
				addParagraphs(currentTask);

		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not complete brackets: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	public String completeBracketsOfString(String InputString) {
		String outputString = "";
		String[] txtBoxTxtArray = InputString.split("\\.");
		for (String txtBoxTxtArrayStr : txtBoxTxtArray) {
			int br1num = 0;// ()
			int br2num = 0;// []
			int br3num = 0;// {}
			//int br4num = 0;// ""
			String bracketsToAdd = ""; // brackets String
			String newtxtBoxTxtArrayStr = "";
			boolean ignoreBr4 = false;
			if ((txtBoxTxtArrayStr.length() - txtBoxTxtArrayStr.replace("\"", "").length()) % 2 == 0)
				ignoreBr4 = true;

			for (int i = 0; i < txtBoxTxtArrayStr.length(); i++) {
				char c = txtBoxTxtArrayStr.charAt(i);
				boolean dontaddchar = false;
				// brackets open; add to brackets String
				if (c == ('(')) {
					br1num++;
					bracketsToAdd = ")" + bracketsToAdd;
				} else if (c == ('[')) {
					br2num++;
					bracketsToAdd = "]" + bracketsToAdd;
				} else if (c == ('{')) {
					br3num++;
					bracketsToAdd = "}" + bracketsToAdd;
				} else if (c == ('"') && !ignoreBr4) {
					ignoreBr4 = true;
					//br4num++;
					bracketsToAdd = "\"" + bracketsToAdd;
				}
				// opened bracket is closed; remove from brackets String
				else if (c == (')') && br1num > 0) {
					br1num--;
					StringBuilder b = new StringBuilder(bracketsToAdd);
					b.replace(bracketsToAdd.lastIndexOf(")"), bracketsToAdd.lastIndexOf(")") + 1, "");
					bracketsToAdd = b.toString();
				} else if (c == (']') && br2num > 0) {
					br2num--;
					StringBuilder b = new StringBuilder(bracketsToAdd);
					b.replace(bracketsToAdd.lastIndexOf("]"), bracketsToAdd.lastIndexOf("]") + 1, "");
					bracketsToAdd = b.toString();
				} else if (c == ('}') && br3num > 0) {
					br3num--;
					StringBuilder b = new StringBuilder(bracketsToAdd);
					b.replace(bracketsToAdd.lastIndexOf("}"), bracketsToAdd.lastIndexOf("}") + 1, "");
					bracketsToAdd = b.toString();
				}
				// remove closed brackets (without opened ones)
				else if (c == (')') && br1num == 0)
					dontaddchar = true;
				else if (c == (']') && br2num == 0)
					dontaddchar = true;
				else if (c == ('}') && br3num == 0)
					dontaddchar = true;

				if (!dontaddchar)
					newtxtBoxTxtArrayStr = newtxtBoxTxtArrayStr + c;
			}
			outputString = outputString + newtxtBoxTxtArrayStr + bracketsToAdd + ".";
		}
		return outputString;
	}

	public void saveTxtboxText() {
		String txtboxtxt = txtBox.getText();

		try {
			Writer writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream("Magazin/txtBoxTxtPersistent"), "utf-8"));
			writer.write(txtboxtxt);
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error!", "Could not save TextBox text: \n" + e.getMessage(),
					e.getLocalizedMessage());
		}
	}

	@FXML
	public void loadTextFromWebsite() {

		// solve encoding issues
		String urlText = fieldUrl.getText().replace("%E2%80%93", "-").replace("%27", "'");

		String baseDomain[] = { "Error" };
		try {
			baseDomain[0] = getDomainName(urlText);
		} catch (URISyntaxException e1) {
			baseDomain[0] = "Error";
			e1.printStackTrace();
			DialogBoxes.showErrorBox("Error!", e1.getMessage(), e1.getLocalizedMessage());
		}

		final int[] loadFromLinks = { 1 };
		if (domain2.isSelected())
			loadFromLinks[0] = 2;
		else if (domain3.isSelected())
			loadFromLinks[0] = 3;

		if (urlText.equals("") || urlText.isEmpty() || baseDomain[0].equals("Error")) {
			DialogBoxes.showErrorBox("Error!", "Error: URL field contains no valid link",
					"Please enter an URL or search term to load text from the internet");
		} else {
			addToLog("Loading website into txtBox: " + urlText);
			final Runnable runnable = new Runnable() {
				public void run() {

					Double progressValue = 0.05;
					progressBar.setVisible(true);
					showProgressBarText("Loading text from URL");
					setProgress(progressValue);

					String speakText = "";
					Document doc;
					try {
						// get first website text + links
						showProgressBarText("Loading text from " + urlText);
						doc = Jsoup.connect(urlText).userAgent(
								"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
								.referrer("http://www.google.com").timeout(9000).get();

						Elements links = doc.select("a[href]");
						if (doc != null) {
							for (Element element : doc.select("p")) {
								speakText = speakText + (element.text());
							}
						}
						if (speakText.equals("") || speakText.isEmpty()) {
							if (doc != null) {
								for (Element element : doc.select("html")) {
									speakText = speakText + (element.text());
									speakText = speakText.replaceAll("<[^>]*>", "");
								}
							}
						}
						// get all connected website texts
						int turnNumber = 0;
						double wordsPerProgress = links.size() / 95;
						for (Element link : links) {
							turnNumber++;
							if (turnNumber % wordsPerProgress == 0) {
								progressValue = progressValue + 0.01;
								setProgress(progressValue);
							}
							String linkAsString = link.attr("abs:href");

							if (loadFromLinks[0] == 3
									|| loadFromLinks[0] == 2 && linkAsString.contains(baseDomain[0])) {

								try {
									addToLog("Loading sub-website into txtBox: " + linkAsString);
								} catch (Exception e) {
									e.printStackTrace();
									DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
								}
								try {
									showProgressBarText("Loading text from " + linkAsString);
									doc = Jsoup.connect(linkAsString).userAgent(
											"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
											.referrer("http://www.google.com").timeout(9000).get();

									if (doc != null) {
										for (Element element : doc.select("p")) {
											speakText = speakText + (element.text());
										}
									}
									if (speakText.equals("") || speakText.isEmpty()) {
										if (doc != null) {
											for (Element element : doc.select("html")) {
												speakText = speakText + (element.text());
												speakText = speakText.replaceAll("<[^>]*>", "");
											}
										}
									}
									// save text persistently:
									Writer writer;
									try {
										writer = new BufferedWriter(new OutputStreamWriter(
												new FileOutputStream("Magazin/txtBoxTxtPersistent"), "utf-8"));
										writer.write(speakText);
										writer.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
									txtBox.setText(speakText);
								} catch (Exception e) {
									try {
										addToLog("Couldn't load text of " + linkAsString + "\n"
												+ e.getLocalizedMessage());
									} catch (Exception ex) {
										ex.printStackTrace();
										DialogBoxes.showErrorBox("Error!", ex.getMessage(), ex.getLocalizedMessage());
									}
									DialogBoxes.showErrorBox("Error!", e.getMessage(), e.getLocalizedMessage());
									e.printStackTrace();
								}
							}
						}

						DialogBoxes.showMessageBox("Process finished!", "The text was loaded intothe text area.", "");
						txtBox.setText(speakText);

						progressBar.setVisible(false);
						hideProgressBarText();
					} catch (IOException | IllegalArgumentException e) {
						e.printStackTrace();
						DialogBoxes.showErrorBox("Error!", "Website URL not found: " + urlText + "\n" + e.getMessage(),
								"Please reformat and try again.\n\nYou can try to copy-paste the URL directly from the browser URL bar.");
					}
				}
			};
			Thread articleDeleteThread = new Thread(runnable);
			articleDeleteThread.start();
		}
	}

	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	// save the current text of the textarea to a TXT file
	@FXML
	public void saveTxtFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save File");
		fileChooser.setInitialFileName("*.txt");

		// remember last folder & show it to the user
		if (currentFolder.contentEquals("user.home"))
			fileChooser.setInitialDirectory(new File(System.getProperty(currentFolder)));
		else
			fileChooser.setInitialDirectory(new File(currentFolder));

		File file = fileChooser.showSaveDialog(new Stage());
		currentFolder = file.getParent().toString();

		saveFileContent(file, txtBox.getText(), true);
	}

	public void saveFileContent(File file, String content, boolean showMessagebox) {
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(file, false);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.print(content); // New line
			printWriter.close();

			if (showMessagebox)
				DialogBoxes.showMessageBox("File saved!", "\"" + file + "\" was stored successfully!", "");
		} catch (IOException e) {
			e.printStackTrace();
			DialogBoxes.showErrorBox("Error (IOException)", e.toString(), "Could not save file.");
		}
	}

	public String createLatexCode(ArrayList<String> wordList, String randomWord, String booktext, double progressValue,
			double wordsPerProgress, int wordCount) {
		Calendar cal = Calendar.getInstance();

		String latexCode = "U92U100U111U99U117U109U101U110U116U99U108U97U115U115U123U97U114U116U105U99U108U101U125U60U98U114U62U92U117U115U101U112U97U99U107U97U103U101U91U117U116U102U56U93U123U105U110U112U117U116U101U110U99U125U60U98U114U62U60U98U114U62U92U116U105U116U108U101U123"
				+ convertToUni2016ghNow(fieldTitle.getText()) + "U125U60U98U114U62U92U97U117U116U104U111U114U123"
				+ convertToUni2016ghNow(fieldAuthor.getText()) + "U125U60U98U114U62U92U100U97U116U101U123"
				+ convertToUni2016ghNow(new SimpleDateFormat("MMMM").format(cal.getTime()))
				+ "U60U115U112U97U99U101U115U105U103U110U62"
				+ convertToUni2016ghNow(String.valueOf(cal.get(Calendar.YEAR)))
				+ "U125U60U98U114U62U60U98U114U62U92U117U115U101U112U97U99U107U97U103U101U91U117U116U102U56U93U123U105U110U112U117U116U101U110U99U125U60U98U114U62U92U117U115U101U112U97U99U107U97U103U101U91U101U110U103U108U105U115U104U93U123U98U97U98U101U108U125U60U98U114U62U92U117U115U101U112U97U99U107U97U103U101U123U110U97U116U98U105U98U125U60U98U114U62U92U117U115U101U112U97U99U107U97U103U101U123U103U114U97U112U104U105U99U120U125U60U98U114U62U92U117U115U101U112U97U99U107U97U103U101U123U109U117U108U116U105U99U111U108U125U60U98U114U62U92U115U101U116U108U101U110U103U116U104U123U92U99U111U108U117U109U110U115U101U112U125U123U49U99U109U125U60U98U114U62U92U117U115U101U112U97U99U107U97U103U101U91U97U52U112U97U112U101U114U44U U32U116U111U116U97U108U61U123U54U105U110U44U U32U56U105U110U125U93U123U103U101U111U109U101U116U114U121U125U60U98U114U62U60U98U114U62U92U98U101U103U105U110U123U100U111U99U117U109U101U110U116U125U60U98U114U62U60U98U114U62U92U109U97U107U101U116U105U116U108U101U60U98U114U62U60U98U114U62U92U98U101U103U105U110U123U97U98U115U116U114U97U99U116U125U60U98U114U62"
				+ createTextQuickly(wordList, randomWord, booktext, progressValue, wordsPerProgress, 200)
				+ "U60U98U114U62U92U101U110U100U123U97U98U115U116U114U97U99U116U125U60U98U114U62U60U98U114U62U92U98U101U103U105U110U123U109U117U108U116U105U99U111U108U115U125U123U50U125U60U98U114U62U60U98U114U62U92U115U101U99U116U105U111U110U123U73U110U116U114U111U100U117U99U116U105U111U110U125U60U98U114U62"
				+ createTextQuickly(wordList, randomWord, booktext, progressValue, wordsPerProgress, 200)
				+ "U60U98U114U62U60U98U114U62U92U101U110U100U123U109U117U108U116U105U99U111U108U115U125U60U98U114U62U92U98U101U103U105U110U123U102U105U103U117U114U101U125U91U104U33U93U60U98U114U62U92U99U101U110U116U101U114U105U110U103U60U98U114U62U92U105U110U99U108U117U100U101U103U114U97U112U104U105U99U115U91U115U99U97U108U101U61U50U93U123U117U110U105U118U101U114U115U101U125U60U98U114U62U92U99U97U112U116U105U111U110U123U84U104U101U U32U85U110U105U118U101U114U115U101U125U60U98U114U62U92U108U97U98U101U108U123U102U105U103U58U117U110U105U118U101U114U115U101U125U60U98U114U62U92U101U110U100U123U102U105U103U117U114U101U125U60U98U114U62U92U98U101U103U105U110U123U109U117U108U116U105U99U111U108U115U125U123U50U125U60U98U114U62U60U98U114U62U37U77U97U105U110U U32U112U97U114U116U60U98U114U62U92U115U101U99U116U105U111U110U123U67U104U97U108U108U101U110U103U101U115U U32U97U110U100U U32U79U112U112U111U114U116U117U110U105U116U105U101U115U125U60U98U114U62"
				+ createTextQuickly(wordList, randomWord, booktext, progressValue, wordsPerProgress, wordCount)
				+ "U60U98U114U62U60U98U114U62U92U115U101U99U116U105U111U110U123U67U111U110U99U108U117U115U105U111U110U125U60U98U114U62"
				+ createTextQuickly(wordList, randomWord, booktext, progressValue, wordsPerProgress, 200)
				+ "U92U99U105U116U101U112U123U97U100U97U109U115U49U57U57U53U104U105U116U99U104U104U105U107U101U114U125U60U98U114U62U60U98U114U62U92U101U110U100U123U109U117U108U116U105U99U111U108U115U125U60U98U114U62U60U98U114U62U92U98U105U98U108U105U111U103U114U97U112U104U121U115U116U121U108U101U123U112U108U97U105U110U125U60U98U114U62U92U98U105U98U108U105U111U103U114U97U112U104U121U123U114U101U102U101U114U101U110U99U101U115U125U60U98U114U62U92U101U110U100U123U100U111U99U117U109U101U110U116U125U60U98U114U62";

		return latexCode;
	}

	public String convertToUni2016ghNow(String s) {
		// String[] words = s.split(" ");
		String str = "";

		for (int i = 0; i < s.length(); i++) {

			int char1 = s.charAt(i);
			if (char1 == 32 || char1 == 10)
				str = str + "U" + " ";
			str = str + "U" + char1;
		}
		str = str.replace("U46", " U46 ");
		str = str.replaceAll("  ", " ");

		return str;
	}
}
