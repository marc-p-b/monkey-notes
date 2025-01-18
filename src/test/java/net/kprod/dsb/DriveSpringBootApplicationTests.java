package net.kprod.dsb;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JsonContent;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//@SpringBootTest
class DriveSpringBootApplicationTests {

	//@Test
	void contextLoads() {

		Path destPath = Paths.get("/tmp", "1n-aOk4hmGsHo393Sl5x71ED2Im9-yIct");
		//Path workingDir = Paths.get("/tmp/1n-aOk4hmGsHo393Sl5x71ED2Im9-yIct");

		//LOG.info("Ls {}", workingDir);
		String argImages = Arrays.stream(destPath.toFile().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".png");
			}
		}))
		.map(File::getAbsolutePath)
		.sorted()
		.collect(Collectors.joining(","));

		System.out.println(argImages);

	}
	//{"fileId": "aaa2233", "fileName": "aaaa", "text-content": [[{"file": "/tmp/gradio/fe54c96c7be7e2eea17a636136a1614a2061d6386347e9767e06d65e8e1a162f/a-1.png", "alt_text": ""}, null], ["image to text", "The text in the image is:\n\n\"n'\u00e9ions des espoirs ?\""]]}
	//{'fileId': '1ayhzqdX1sFzTXXCM49BKlgEt4oP23O4G', 'fileName': 'pdf', 'text-content':[[{'file': '/tmp/gradio/69b02b88edf59176e0f600ed5502eac85cd8c8a3cd7680b08beab40325725d55/codir 250108.pdf-1.png', 'alt_text': ''}, None],[{'file': '/tmp/gradio/ed4ea45d7c11267638951d01c60e1d500f2cf869af4c2bea43e5b6498fa03581/codir 250108.pdf-2.png', 'alt_text': ''}, None],['image to text', '### Image 1\n\n- Formation\n  - un collab doit recevoir une formation tous les 6 mois\n  - voeux (Robin ?)\n  - coop fournisseurs\n    - mail envoyé / 30k à ce jour\n\nPoint MARC\n- Migration\n- SMS / T2C\n- Formation tech\n- Restructuration Ne@S6\n\nCA travaux | 20M 217\n| 59500 lead\n| 33,4% publi\n| 12,9% sign\n\n### Image 2\n\nPrépa Présentation\n- Bilan par équipe\n  - Acquis | Bilans chiffrés\n  - Pro / but\n  - Effectifs\n- Budget\n- Identification Qu\n  - commune\n  - tech / produit\n\npubli auto - analyse des commentaires\nassurance x dégâts-des-eaux\nconseil / banque\n-> revoir prez data']]}
	//{"fileId": "17PtS0N0iNoRS2ZwknhZPydHxWgBkVOk2", "fileName": "pdf", "text-content":
	// [[{"file": "/tmp/gradio/81fb23170c7080a5083b5f87468f4bafcb8fbcb62aa2a9a346ca7f747931dad1/230325 - codir.pdf-1.png", "alt_text": ""}, null],
	// ["image to text", "Sure, here is the text from the image:\n\n```\n[] CR\n- NAO : ou / sans Nanc des\n- code pharma petites images <7 24/09\n- fiabilit\u00e9s des pros\n- UPI -> activit\u00e9 commune\n- Journ\u00e9e du 24/14\n\n-> pr\u00e9pa \u00e0 l'avance\n-> soumission et pr\u00e9 travail voir\n-> pr\u00e9sentation de la zone\nl'eau fonde\n\n-> r\u00e9org des espaces ?\n```"]]}




	@Test
	void test() throws IOException {
//		String json = "{\"fileId\": \"16ly0puSsJDeUglrCKsWWfkEkfqVgDrZ5\", \"fileName\": \"codir 250108\", \"text-content\": [[{\"file\": \"/tmp/gradio/69b02b88edf59176e0f600ed5502eac85cd8c8a3cd7680b08beab40325725d55/codir 250108.pdf-1.png\", \"alt_text\": \"\"}, null], [{\"file\": \"/tmp/gradio/ed4ea45d7c11267638951d01c60e1d500f2cf869af4c2bea43e5b6498fa03581/codir 250108.pdf-2.png\", \"alt_text\": \"\"}, null], [\"image to text\", \"### Image 1\\n\\n- Formation\\n  - Un collab doit recevoir une formation tous les 6 mois\\n  - Voemy (Robin ?)\\n  - Coop fournisseurs\\n    - Mise en place / 30k \\u00e0 ce jour\\n\\nPoint MARC\\n- Migration\\n- SMS / T2C\\n- Formation tech\\n- Restructuration Ne@S6\\n\\nCA travaux | 20M 217\\n| 59500 lead\\n| 33,4% publi\\n| 12,9% sign\\n\\n### Image 2\\n\\nPr\\u00e9pa Pr\\u00e9sentation\\n- Bilan par \\u00e9quipe\\n  - Acquis | Bilans chiffr\\u00e9s\\n  - Probl\\u00e8mes\\n  - Effectifs\\n- Budget\\n- Identification Qu\\n  - Commune\\n  - Tech / Produit\\n\\npubli auto - analyse des commentaires\\n- Assurances x d\\u00e9g\\u00e2ts des eaux\\n- Conseil / Banque\\n  - Revoir prez data\"]]}\n";
//
//		DocumentContext context = JsonPath.parse(json);
//
//		String fileId = context.read("$.fileId");
//		String fileName = context.read("$.fileName");
//
//		String transcript = context.read("$.text-content[-1][-1]");
//
//		transcript = transcript.replaceAll("[\\p{Cc}&&[^\\r\\n]]|[\\p{Cf}\\p{Co}\\p{Cn}]", "(!)");

		String t2 = "### Image 1\n\n- Formation\n  - Un collab doit recevoir une formation tous les 6 mois\n  - Voemy (Robin ?)\n  - Coop fournisseurs\n    - Mise en place / 30k à ce jour\n\nPoint MARC\n- Migration\n- SMS / T2C\n- Formation tech\n- Restructuration Ne@S6\n\nCA travaux | 20M 217\n| 59500 lead\n| 33,4% publi\n| 12,9% sign\n\n### Image 2\n\nPrépa Présentation\n- Bilan par équipe\n  - Acquis | Bilans chiffrés\n  - Problèmes\n  - Effectifs\n- Budget\n- Identification Qu\n  - Commune\n  - Tech / Produit\n\npubli auto - analyse des commentaires\n- Assurances x dégâts des eaux\n- Conseil / Banque\n  - Revoir prez data\n";
		//t2 = t2.text.replace("\n", "").replace("\r", "");
		//System.out.println(t2);

		createTranscriptPdf("aaa333", t2);
	}

	public java.io.File createTranscriptPdf(String fileId, String textContent) throws IOException {

		//textContent = textContent.replaceAll("\\p{C}", "");
		textContent = textContent.replaceAll("[\\p{Cc}&&[^\\r\\n]]|[\\p{Cf}\\p{Co}\\p{Cn}]", "(!)");

		textContent.replaceAll("###", "\n\n###");

		PDDocument doc = null;
		try
		{
			doc = new PDDocument();
			PDPage page = new PDPage();
			doc.addPage(page);
			PDPageContentStream contentStream = new PDPageContentStream(doc, page);

			PDFont pdfFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);;
			float fontSize = 12;
			float leading = 1.5f * fontSize;

			PDRectangle mediabox = page.getMediaBox();
			float margin = 20;
			float width = mediabox.getWidth() - 2*margin;
			float startX = mediabox.getLowerLeftX() + margin;
			float startY = mediabox.getUpperRightY() - margin;

			String text = textContent;
			List<String> lines = new ArrayList<>();
			int lastSpace = -1;
			float size = 0;
			String subString;
			while (text.length() > 0)
			{
				//int nlIndex = text.indexOf('\n', lastSpace + 1);
				int nlIndex = text.indexOf('\n');
				int spaceIndex = text.indexOf(' ', lastSpace + 1);

				if (spaceIndex < 0) {
					spaceIndex = text.length();
				}

				subString = text.substring(0, spaceIndex > nlIndex ? spaceIndex : nlIndex);
				size = fontSize * pdfFont.getStringWidth(subString.replaceAll("\\p{C}", "")) / 1000;

				if (nlIndex < spaceIndex && nlIndex != -1) {
					subString = text.substring(0, nlIndex);

					lines.add(subString.replaceAll("\\p{C}", ""));
					text = text.substring(nlIndex).trim();
					System.out.printf("> %s\n", subString);
				} else if (size > width) {
					if (lastSpace < 0) {
						lastSpace = spaceIndex;
					}
					subString = text.substring(0, lastSpace);

					lines.add(subString.replaceAll("\\p{C}", ""));
					text = text.substring(lastSpace).trim();
					System.out.printf("> %s\n", subString);
					lastSpace = -1;
				}
				else if (spaceIndex == text.length()) {
					lines.add(text.replaceAll("\\p{C}", ""));
					System.out.printf(">%s\n", text);
					text = "";
				}
				else {
					lastSpace = spaceIndex;
				}
			}

			contentStream.beginText();
			contentStream.setFont(pdfFont, fontSize);
			contentStream.newLineAtOffset(startX, startY);
			for (String line: lines)
			{
				contentStream.showText(line);
				contentStream.newLineAtOffset(0, -leading);
			}
			contentStream.endText();
			contentStream.close();

			doc.save(new File("/tmp/", "file-t" + ".pdf"));
		}
		finally
		{
			if (doc != null)
			{
				doc.close();
			}
		}




		//contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);


		//return path.toFile();
		return null;
	}


}
