package net.kprod.dsb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

//@SpringBootTest
class DriveSpringBootApplicationTests {

	@Test
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

}
