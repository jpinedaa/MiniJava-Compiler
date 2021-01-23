import syntaxtree.*;
import java.io.*;

public class Main {
	public static void main(String[] args) {

			FileInputStream fis = null;
			try {		
							
				MiniJavaParser parser = new MiniJavaParser(System.in);
				Goal root = parser.Goal();
				
				FirstPassVisitor visitor1 = new FirstPassVisitor();
				root.accept(visitor1, null);

				SecondPassVisitor visitor2 = new SecondPassVisitor(visitor1.classes);
				root.accept(visitor2, null);
				
				File newFile = new File("output.txt");
				newFile.setWritable(true);
				FileOutputStream file = new FileOutputStream(newFile);
				file.write(visitor2.finalSpigletCode.getBytes());
				file.close();
				
				System.out.println("Intermediate Representation finished!");
			} catch (ParseException ex) {
				System.out.println(ex.getMessage());
			} catch (FileNotFoundException ex) {
				System.err.println(ex.getMessage());
			} catch (Exception ex) { 
				System.out.println(ex.getMessage());
			} 
		
	}
}
