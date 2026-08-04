package com.hospital.vaccination.ui;

import com.hospital.vaccination.model.Child;
import com.hospital.vaccination.model.VaccinationRecord;
import com.hospital.vaccination.service.PdfCardService;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.BSDateConverter.BSDate;
import com.hospital.vaccination.util.DateUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Shared "vaccination card" popup used from two places:
 *   • right after a nurse registers a child (ChildRegistrationPanel)
 *   • when admin clicks "Reprint card" (AllChildrenPanel)
 *
 * <p>Shows a scrollable schedule table + three buttons:
 * <b>Print now</b> / <b>Save PDF card…</b> / <b>Skip</b>.
 *
 * <p>All rendering + printing logic lives here, so both call sites
 * behave identically and any future tweak is a one-file change.
 */
public final class CardActionDialog {

    private static final PdfCardService pdfService = new PdfCardService();

    private CardActionDialog() { }

    /**
     * Pop up the schedule + action dialog for the given child.
     *
     * @param parent          Swing parent (any panel/frame)
     * @param title           dialog window title (e.g. "Registration successful"
     *                        or "Vaccination card — Aarav Sharma")
     * @param leadIn          optional HTML shown above the schedule table
     *                        (e.g. "Aarav Sharma registered successfully.")
     *                        Pass null or "" to omit.
     */
    public static void show(Component parent, Child child,
                            List<VaccinationRecord> records,
                            String title, String leadIn) {

        JScrollPane scroll = buildContent(child, records, leadIn);

        Object[] options = { "Print now", "Save PDF card...", "Skip" };
        int choice = JOptionPane.showOptionDialog(
                parent,
                scroll,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {           // Print now
            printPdf(parent, child, records);
        } else if (choice == 1) {    // Save PDF card...
            savePdf(parent, child, records);
        }
    }

    // ---------------------------------------------------------- content

    private static JScrollPane buildContent(Child child,
                                            List<VaccinationRecord> records,
                                            String leadIn) {
        BSDate bs = BSDateConverter.toBS(child.getDateOfBirth());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:520px;'>");

        if (leadIn != null && !leadIn.isBlank()) {
            sb.append(leadIn).append("<br>");
        }

        sb.append("<b style='font-size:12pt;'>").append(escape(child.getName()))
                .append("</b><br>");
        sb.append("DOB: ").append(DateUtil.display(child.getDateOfBirth()))
                .append(" &nbsp;/&nbsp; ").append(bs.display()).append(" BS<br>");
        sb.append("Parent: ").append(escape(child.getParentName()))
                .append(" &nbsp;—&nbsp; ").append(escape(child.getParentPhone()))
                .append("<br><br>");

        sb.append(records.size()).append(" scheduled doses:<br><br>");

        sb.append("<table cellpadding='4' cellspacing='0' border='1' " +
                "style='border-collapse:collapse;'>");
        sb.append("<tr bgcolor='#0066B2' style='color:white;'>" +
                "<th align='left'>#</th>" +
                "<th align='left'>Vaccine</th>" +
                "<th align='left'>Due (AD)</th>" +
                "<th align='left'>Due (BS)</th>" +
                "<th align='left'>Status</th></tr>");
        int i = 1;
        boolean zebra = false;
        for (VaccinationRecord rec : records) {
            String bg = zebra ? " bgcolor='#F0F6FC'" : "";
            zebra = !zebra;
            BSDate bsDue = BSDateConverter.toBS(rec.getDueDate());
            String statusColor = switch (rec.getStatus()) {
                case COMPLETED -> "#008200";
                case MISSED    -> "#B00020";
                default        -> "#B47800";
            };
            sb.append("<tr").append(bg).append(">")
                    .append("<td>").append(i++).append("</td>")
                    .append("<td>").append(rec.getVaccineName()).append("</td>")
                    .append("<td>").append(DateUtil.display(rec.getDueDate())).append("</td>")
                    .append("<td>").append(bsDue.format()).append("</td>")
                    .append("<td><b style='color:").append(statusColor).append(";'>")
                    .append(rec.getStatus().name()).append("</b></td>")
                    .append("</tr>");
        }
        sb.append("</table>");
        sb.append("<br><i>Print the card now, save a PDF to keep, or skip.</i>");
        sb.append("</body></html>");

        JLabel content = new JLabel(sb.toString());
        JScrollPane scroll = new JScrollPane(content);
        scroll.setPreferredSize(new Dimension(620, 460));
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ---------------------------------------------------------- save PDF

    private static void savePdf(Component parent, Child child,
                                List<VaccinationRecord> records) {
        String safeName = child.getName().replaceAll("[^A-Za-z0-9]+", "_");
        String suggested = "VaccinationCard_" + safeName + "_" +
                child.getDateOfBirth() + ".pdf";

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save vaccination card");
        chooser.setSelectedFile(new File(suggested));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File out = chooser.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".pdf")) {
            out = new File(out.getParentFile(), out.getName() + ".pdf");
        }

        try {
            pdfService.generate(out, child, records);
        } catch (Exception ex) {
            ex.printStackTrace();
            error(parent, "Could not save PDF: " + ex.getMessage());
            return;
        }

        int open = JOptionPane.showConfirmDialog(
                parent,
                "PDF saved to:\n" + out.getAbsolutePath() + "\n\nOpen it now?",
                "Card saved",
                JOptionPane.YES_NO_OPTION);
        if (open == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
            try { Desktop.getDesktop().open(out); }
            catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // ---------------------------------------------------------- print PDF

    private static void printPdf(Component parent, Child child,
                                 List<VaccinationRecord> records) {
        try {
            String safeName = child.getName().replaceAll("[^A-Za-z0-9]+", "_");
            File tmp = File.createTempFile(
                    "VaccinationCard_" + safeName + "_", ".pdf");
            tmp.deleteOnExit();
            pdfService.generate(tmp, child, records);

            try (org.apache.pdfbox.pdmodel.PDDocument doc =
                         org.apache.pdfbox.Loader.loadPDF(tmp)) {

                java.awt.print.PrinterJob job =
                        java.awt.print.PrinterJob.getPrinterJob();
                job.setJobName("Vaccination Card - " + child.getName());
                job.setPageable(new org.apache.pdfbox.printing.PDFPageable(doc));

                if (job.printDialog()) {
                    job.print();
                    JOptionPane.showMessageDialog(
                            parent,
                            "Card sent to printer for " + child.getName() + ".",
                            "Printing",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (java.awt.print.PrinterException pe) {
            pe.printStackTrace();
            error(parent, "Printer error: " + pe.getMessage() +
                    "\n\nIs a printer installed and turned on?");
        } catch (Exception ex) {
            ex.printStackTrace();
            error(parent, "Could not print: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------- helpers

    private static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg,
                "Error", JOptionPane.WARNING_MESSAGE);
    }

    private static String escape(String s) {
        return s == null ? "" : s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
