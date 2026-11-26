package com.nmamit.payments.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nmamit.payments.model.entity.Approval;
import com.nmamit.payments.model.entity.PaymentRequest;
import com.nmamit.payments.model.entity.PaymentTransaction;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds the one-page PDF payment receipt (OpenPDF). Kept small (no images, built-in fonts)
 * so it fits comfortably within email attachment limits.
 */
@Service
public class ReceiptService {

    private static final Color BLUE = new Color(0x1f, 0x3b, 0x73);
    private static final Color BLUE_LIGHT = new Color(0xe8, 0xee, 0xfb);
    private static final Color LINE = new Color(0xdd, 0xe3, 0xf0);
    private static final Color MUTED = new Color(0x71, 0x80, 0x96);
    private static final Color GREEN = new Color(0x1e, 0x7d, 0x34);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Font TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE);
    private static final Font WHITE = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE);
    private static final Font H = new Font(Font.HELVETICA, 11, Font.BOLD, BLUE);
    private static final Font LABEL = new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED);
    private static final Font VALUE = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font VALUE_B = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
    private static final Font TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, BLUE);
    private static final Font OK = new Font(Font.HELVETICA, 10, Font.BOLD, GREEN);
    private static final Font SMALL = new Font(Font.HELVETICA, 8, Font.ITALIC, MUTED);

    /** e.g. RCPT-2026-0007 */
    public static String receiptNumber(PaymentTransaction tx) {
        int year = tx.getPaidAt() != null ? tx.getPaidAt().getYear() : LocalDateTime.now().getYear();
        return String.format("RCPT-%d-%04d", year, tx.getId());
    }

    public byte[] generate(PaymentTransaction tx, PaymentRequest r, List<Approval> approvals) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 36, 36);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Payment receipt " + receiptNumber(tx));
        doc.addCreator("Corporate Payments");
        doc.open();

        LocalDateTime paidAt = tx.getPaidAt() != null ? tx.getPaidAt() : LocalDateTime.now();

        // ---- header band ----
        PdfPTable header = new PdfPTable(new float[]{3, 2});
        header.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.addElement(new Paragraph("PAYMENT RECEIPT", TITLE));
        left.addElement(new Paragraph("Corporate Payments - Approval and Management System", WHITE));
        PdfPCell right = new PdfPCell();
        Paragraph rn = new Paragraph("Receipt no. " + receiptNumber(tx), new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE));
        rn.setAlignment(Element.ALIGN_RIGHT);
        Paragraph rd = new Paragraph("Paid on " + paidAt.format(WHEN), WHITE);
        rd.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(rn);
        right.addElement(rd);
        for (PdfPCell c : new PdfPCell[]{left, right}) {
            c.setBackgroundColor(BLUE);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(14);
            header.addCell(c);
        }
        doc.add(header);
        doc.add(spacer(10));

        // ---- status line ----
        Paragraph status = new Paragraph();
        status.add(new Chunk("PAID  ", OK));
        status.add(new Chunk("Transaction reference " + tx.getReferenceNumber() + "   |   OTP verified: "
                + (tx.isOtpVerified() ? "Yes" : "No"), VALUE));
        doc.add(status);
        doc.add(spacer(8));

        // ---- paid to / against ----
        PdfPTable two = new PdfPTable(2);
        two.setWidthPercentage(100);
        two.setSpacingAfter(10);
        two.addCell(box("PAID TO",
                new String[][]{
                        {"Beneficiary", r.getBeneficiaryName()},
                        {"Account", mask(tx.getPaidToAccount())}}));
        two.addCell(box("AGAINST",
                new String[][]{
                        {"Request no.", r.getRequestNumber()},
                        {"Title", r.getTitle()},
                        {"Category", r.getCategory() == null ? "-" : r.getCategory().name()},
                        {"Raised by", r.getRequestedBy().getFullName()},
                        {"Due date", r.getDueDate() == null ? "-" : r.getDueDate().format(DAY)}}));
        doc.add(two);

        // ---- amounts ----
        PdfPTable amt = new PdfPTable(new float[]{3, 1.4f});
        amt.setWidthPercentage(100);
        amountRow(amt, "Description", "Amount (INR)", true);
        amountRow(amt, r.getTitle(), money(r.getAmount()), false);
        PdfPCell tl = new PdfPCell(new Phrase("Total paid", TOTAL));
        PdfPCell tv = new PdfPCell(new Phrase("INR " + money(tx.getAmount()), TOTAL));
        tv.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[]{tl, tv}) {
            c.setBackgroundColor(BLUE_LIGHT);
            c.setBorderColor(LINE);
            c.setPadding(8);
            amt.addCell(c);
        }
        doc.add(amt);
        Paragraph words = new Paragraph("Amount in words: " + inWords(tx.getAmount()), VALUE_B);
        words.setSpacingBefore(6);
        words.setSpacingAfter(12);
        doc.add(words);

        // ---- approvals ----
        doc.add(new Paragraph("Approval trail", H));
        PdfPTable ap = new PdfPTable(new float[]{1.2f, 2, 1.3f, 2.5f});
        ap.setWidthPercentage(100);
        ap.setSpacingBefore(4);
        for (String h : new String[]{"Level", "Approved by", "Date", "Remarks"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, LABEL));
            c.setBackgroundColor(BLUE_LIGHT);
            c.setBorderColor(LINE);
            c.setPadding(5);
            ap.addCell(c);
        }
        for (Approval a : approvals) {
            ap.addCell(cell(a.getLevel().name()));
            ap.addCell(cell(a.getApprover().getFullName()));
            ap.addCell(cell(a.getDecidedAt() == null ? "-" : a.getDecidedAt().format(DAY)));
            ap.addCell(cell(a.getRemarks() == null ? "-" : a.getRemarks()));
        }
        doc.add(ap);
        Paragraph by = new Paragraph("Payment made by " + tx.getProcessedBy().getFullName() + " (Finance) on "
                + paidAt.format(WHEN) + ".", VALUE);
        by.setSpacingBefore(10);
        doc.add(by);

        // ---- footer ----
        Paragraph foot = new Paragraph("This is a computer-generated receipt and does not require a signature. "
                + "Quote the transaction reference " + tx.getReferenceNumber() + " in any query.", SMALL);
        foot.setSpacingBefore(24);
        doc.add(foot);

        doc.close();
        return out.toByteArray();
    }

    // ------------------------------------------------------------ helpers

    private static PdfPCell box(String title, String[][] rows) {
        PdfPTable t = new PdfPTable(new float[]{1, 2});
        t.setWidthPercentage(100);
        PdfPCell head = new PdfPCell(new Phrase(title, H));
        head.setColspan(2);
        head.setBorder(Rectangle.NO_BORDER);
        head.setPaddingBottom(4);
        t.addCell(head);
        for (String[] row : rows) {
            PdfPCell l = new PdfPCell(new Phrase(row[0], LABEL));
            PdfPCell v = new PdfPCell(new Phrase(row[1] == null ? "-" : row[1], VALUE));
            l.setBorder(Rectangle.NO_BORDER);
            v.setBorder(Rectangle.NO_BORDER);
            l.setPaddingBottom(3);
            v.setPaddingBottom(3);
            t.addCell(l);
            t.addCell(v);
        }
        PdfPCell wrap = new PdfPCell(t);
        wrap.setBorderColor(LINE);
        wrap.setPadding(8);
        return wrap;
    }

    private static void amountRow(PdfPTable t, String label, String value, boolean head) {
        PdfPCell l = new PdfPCell(new Phrase(label, head ? LABEL : VALUE));
        PdfPCell v = new PdfPCell(new Phrase(value, head ? LABEL : VALUE));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[]{l, v}) {
            c.setBorderColor(LINE);
            c.setPadding(7);
            if (head) {
                c.setBackgroundColor(BLUE_LIGHT);
            }
            t.addCell(c);
        }
    }

    private static PdfPCell cell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, VALUE));
        c.setBorderColor(LINE);
        c.setPadding(5);
        return c;
    }

    private static Paragraph spacer(float h) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(h);
        return p;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String mask(String account) {
        if (account == null || account.length() <= 4) {
            return nz(account);
        }
        return "X".repeat(account.length() - 4) + account.substring(account.length() - 4);
    }

    /** Indian digit grouping, e.g. 2,47,800.00 */
    static String money(BigDecimal amount) {
        BigDecimal a = amount.setScale(2, RoundingMode.HALF_UP);
        String s = a.toPlainString();
        String intPart = s.substring(0, s.indexOf('.'));
        String dec = s.substring(s.indexOf('.'));
        boolean neg = intPart.startsWith("-");
        if (neg) {
            intPart = intPart.substring(1);
        }
        if (intPart.length() > 3) {
            String last3 = intPart.substring(intPart.length() - 3);
            String rest = intPart.substring(0, intPart.length() - 3);
            StringBuilder b = new StringBuilder();
            while (rest.length() > 2) {
                b.insert(0, "," + rest.substring(rest.length() - 2));
                rest = rest.substring(0, rest.length() - 2);
            }
            intPart = rest + b + "," + last3;
        }
        return (neg ? "-" : "") + intPart + dec;
    }

    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    /** e.g. "Rupees Forty-Four Thousand Eight Hundred Forty and Fifty Paise Only" (Indian system: lakh, crore). */
    static String inWords(BigDecimal amount) {
        BigDecimal a = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = a.longValue();
        int paise = a.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String words = rupees == 0 ? "Zero" : indian(rupees);
        return "Rupees " + words + (paise > 0 ? " and " + twoDigits(paise) + " Paise" : "") + " Only";
    }

    private static String indian(long n) {
        StringBuilder b = new StringBuilder();
        long crore = n / 10_000_000;
        n %= 10_000_000;
        long lakh = n / 100_000;
        n %= 100_000;
        long thousand = n / 1000;
        n %= 1000;
        long hundred = n / 100;
        long rest = n % 100;
        if (crore > 0) b.append(indian(crore)).append(" Crore ");
        if (lakh > 0) b.append(twoDigits((int) lakh)).append(" Lakh ");
        if (thousand > 0) b.append(twoDigits((int) thousand)).append(" Thousand ");
        if (hundred > 0) b.append(ONES[(int) hundred]).append(" Hundred ");
        if (rest > 0) b.append(twoDigits((int) rest));
        return b.toString().trim();
    }

    private static String twoDigits(int n) {
        if (n < 20) {
            return ONES[n];
        }
        return TENS[n / 10] + (n % 10 > 0 ? "-" + ONES[n % 10] : "");
    }
}
