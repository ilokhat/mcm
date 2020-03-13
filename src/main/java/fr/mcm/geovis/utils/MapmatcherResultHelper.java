package fr.mcm.geovis.utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class MapmatcherResultHelper {
	
	private static Set<Integer> getListOf250kIds(String col2) {
		Set<Integer> ids = new HashSet<>();
		char firstChar = col2.charAt(0);
		switch (firstChar) {
		case 's':
			break;
		case '{': // no regex this should do the job !
			String subs = col2.substring(1, col2.length() - 1);
			ids = Stream.of(subs.split("'")).filter(item ->  !", ".equals(item) && !"".equals(item))
					.flatMapToInt(num -> IntStream.of(Integer.parseInt(num))).boxed().collect(Collectors.toSet());
//			if (col2.split(", ").length > 2)
//				System.out.println(subs + " " + ids);
			break;
		}
		return ids;
	}

	public static Map<Integer, Set<Integer>> buildmapMatcherResult(String csvResultFile) throws IOException {
		Map<Integer, Set<Integer>> res = new HashMap<Integer, Set<Integer>>();
		Reader in = new FileReader(csvResultFile);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
		int i = 0;
		for (CSVRecord record : records) {
			if (i == 0) {
				++i;
				continue; //skip header
			}
		    String columnOne = record.get(0);
		    int idBduni = Integer.parseInt(columnOne);
		    String columnTwo = record.get(1);
		    Set<Integer> idsBd250k = getListOf250kIds(columnTwo);
		    res.put(idBduni, idsBd250k);
		    //System.out.println(idBduni + " -- "+ idsBd250k);
		}
		return res;
	}
	
	public static void main(String[] args) throws IOException {
		String csvFile = "/home/imran/projets/multicriteriamatching/routes_appariement/results.csv";
		Map<Integer, Set<Integer>> mapping = buildmapMatcherResult(csvFile);
		System.out.println(mapping.size());
	}

}
