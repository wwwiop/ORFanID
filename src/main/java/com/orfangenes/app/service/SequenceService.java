package com.orfangenes.app.service;

import static com.orfangenes.app.util.Constants.*;

import com.orfangenes.app.model.Gene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class SequenceService {

    private String blastType;
    private String sequenceFile;
    private String outputDir;

    String BLAST_LOCATION="/blast/bin/";

    String BLAST_NR_DB_LOCATION = "/nr_db/";
    String BLAST_NT_DB_LOCATION = "/nt_db/";


    public SequenceService(String blastType, String sequenceFile, String outputDir) {
        this.blastType = blastType;
        this.sequenceFile = sequenceFile;
        this.outputDir = outputDir;
    }

    public void findHomology( String out, int maxTargetSeqs, int eValue){
//        String inputSequence = getSequenceFromFile(this.sequenceFile);
//        List<String> sequenceBatches = separateSequenceToBatches(inputSequence);
//        for (int fileCount = 0; fileCount <sequenceBatches.size() ; fileCount++) {
//            createSequenceFile(out, sequenceBatches.get(fileCount) , fileCount+1);
//        }
        runBlastCommands(maxTargetSeqs, eValue);
//        combineBlastResults(sequenceBatches.size());
    }

    private void runBlastCommands(int maxTargetSeqs, int evalue) {
        log.warn("Running BLAST. Be patient...This will take 2-15 min...");
        long startTime = System.currentTimeMillis();

        BlastCommandRunner blastCommandRunner = new BlastCommandRunner(BLAST_LOCATION, BLAST_NR_DB_LOCATION, BLAST_NT_DB_LOCATION);
        blastCommandRunner.setSequenceType(this.blastType);
        blastCommandRunner.setOut(this.outputDir);
        blastCommandRunner.setMaxTargetSeqs(String.valueOf(maxTargetSeqs));
        blastCommandRunner.setEvalue("1e-" + evalue);
        blastCommandRunner.run();

        long stopTime = System.currentTimeMillis();
        log.info("BLAST successfully Completed!! Time taken: " + (stopTime - startTime) + "ms");
    }

    private void combineBlastResults(int fileCount) {
        // Combining all BLAST results to one file
        try {
            PrintWriter writer = new PrintWriter(outputDir + File.separator + BLAST_RESULTS + BLAST_EXT);
            for (int i = 0; i < fileCount; i++) {
                BufferedReader reader =
                        new BufferedReader(
                                new FileReader(outputDir + File.separator + BLAST_RESULTS + (i+1) + BLAST_EXT));
                String blastResult = reader.readLine();
                while (blastResult != null) {
                    writer.println(blastResult);
                    blastResult = reader.readLine();
                }
                reader.close();
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getSequenceFromFile(String sequenceFileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(sequenceFileName), StandardCharsets.UTF_8)) {
            stream.forEach(s -> stringBuilder.append(s).append(LINE_SEPARATOR));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return stringBuilder.toString();
    }

    private List<String> separateSequenceToBatches(String inputSequence) {

        final int SEQUENCE_BATCH_SIZE = 3;
        List<String> sequenceBatches = new ArrayList<>();

        String[] sequences = inputSequence.split(SEQUENCE_SEPARATOR);
        StringBuilder currentSequence = new StringBuilder();

        for (int i = 1; i <= sequences.length; i++) {
            currentSequence.append(sequences[i - 1]);
            currentSequence.append(SEQUENCE_SEPARATOR);

            if (i % SEQUENCE_BATCH_SIZE == 0 || i == sequences.length) {
                sequenceBatches.add(currentSequence.toString().trim());
            }
        }
        return sequenceBatches;
    }

    private void createSequenceFile(String out, String sequence, int fileNo) {
        try {
            PrintWriter writer =
                    new PrintWriter(out + File.separator + SEQUENCE + fileNo + FASTA_EXT, StandardCharsets.UTF_8.toString());
            writer.println(sequence);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
    }

    public List<Gene> getGenes(int inputTax) {

        List<Gene> genes = new ArrayList<>();

        String inputSequence = getSequenceFromFile(this.sequenceFile);
        String[] sequences = inputSequence.split(SEQUENCE_SEPARATOR);

        for (String sequence : sequences) {
            if (sequence.equals(LINE_SEPARATOR)) {
                continue;
            }
            Gene gene = new Gene();
            String[] lines = sequence.split(LINE_SEPARATOR);

            // 1) process comment line
            String commentLine = lines[0];
            String[] comments = commentLine.split(" ", 2);
            gene.setGeneId(comments[0].substring(1));
            gene.setDescription(comments[1]);
            gene.setTaxonomyId(inputTax);

            // 2) process sequence lines
            StringBuilder sequenceString = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                sequenceString.append(lines[i]);
            }
            gene.setSequence(sequenceString.toString());
            gene.setLength(sequenceString.length());
            genes.add(gene);
        }
        return genes;
    }
}
