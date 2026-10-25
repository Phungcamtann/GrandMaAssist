package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/** Explicit layouts only: legacy seven fields with |, or six fields with ||. Never retain raw QR. */
public final class CardIdentity {
    public static final class ParseFailure extends IllegalArgumentException {
        public final String code;
        ParseFailure(String code) { super(code); this.code=code; }
    }
    public final String fullName, dateOfBirth, documentNumber;
    public final long parserVersion;
    public CardIdentity(String name, String birth, String number) {
        this(name,birth,number,1L);
    }
    private CardIdentity(String name,String birth,String number,long parserVersion) {
        fullName=name;dateOfBirth=birth;documentNumber=number;this.parserVersion=parserVersion;
    }
    public static CardIdentity parse(String raw) {
        if (raw == null || raw.length() > 2048) throw new ParseFailure("FORMAT");
        String value=raw.trim();
        // The six-field layout was supplied by the user, with personal data masked:
        // ID||NAME||ddMMyyyy||GENDER||ADDRESS||ISSUED_DATE||||
        // It has no old-CMND slot. Do not collapse empty slots globally: legacy cards
        // use ID||NAME|DOB|... to represent an empty old-CMND field.
        String[] pair=value.split("\\|\\|",-1);
        if(fieldCount(pair,6)==6 && noEmbeddedPipe(pair))return core(pair,1,2,2L);
        String[] parts=value.split("\\|",-1);
        if(fieldCount(parts,7)!=7)throw new ParseFailure("FORMAT");
        return core(parts,2,3,1L);
    }
    private static int fieldCount(String[] fields,int minimum) {
        int count=fields.length;
        while(count>minimum && fields[count-1].trim().isEmpty())--count;
        return count;
    }
    private static boolean noEmbeddedPipe(String[] fields) {
        for(String field:fields)if(field.indexOf('|')>=0)return false;
        return true;
    }
    private static CardIdentity core(String[] parts,int nameIndex,int birthIndex,long version) {
        String number=parts[0].trim();
        if (!number.matches("[0-9]{12}")) throw new ParseFailure("DOCUMENT_NUMBER");
        String name = parts[nameIndex].trim();
        if (name.isEmpty() || name.length() > 100 || name.chars().anyMatch(Character::isISOControl))
            throw new ParseFailure("NAME");
        String date=parts[birthIndex].trim();
        if (!date.matches("[0-9]{8}")) throw new ParseFailure("BIRTH_DATE");
        LocalDate birth;
        try { birth=LocalDate.parse(date,DateTimeFormatter.ofPattern("ddMMuuuu")
                .withResolverStyle(ResolverStyle.STRICT)); }
        catch(DateTimeParseException e) { throw new ParseFailure("BIRTH_DATE"); }
        if (birth.isAfter(LocalDate.now()) || birth.getYear() < 1)
            throw new ParseFailure("BIRTH_DATE");
        return new CardIdentity(name, birth.toString(), number,version);
    }
    public Map<String, Object> data() {
        Map<String, Object> d = new HashMap<>();
        d.put("fullName", fullName); d.put("dateOfBirth", dateOfBirth);
        d.put("documentNumber", documentNumber); d.put("source", "CCCD_QR");
        d.put("parserVersion", parserVersion); return d;
    }
    /** Fixed labels and counts only. Do not include field values, prefixes, or exact lengths. */
    public static String structureSummary(String raw) {
        if(raw==null || raw.trim().isEmpty())return "kind=EMPTY";
        if(raw.length()>2048)return "kind=OVER_LIMIT";
        String value=raw.trim();
        int pipes=0,semicolons=0,tabs=0,lines=0;
        for(int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if(c=='|')++pipes;
            else if(c==';')++semicolons;
            else if(c=='\t')++tabs;
            else if(c=='\n')++lines;
        }
        String kind=pipes>0?"PIPE":value.startsWith("{")?"OBJECT":value.startsWith("[")?"ARRAY":
                value.startsWith("<")?"XML":value.regionMatches(true,0,"https://",0,8)
                        ||value.regionMatches(true,0,"http://",0,7)?"LINK":"TEXT";
        String[] fields=value.split("\\|",-1);int emptyTail=0;
        for(int i=fields.length-1;i>=0 && fields[i].trim().isEmpty();i--)++emptyTail;
        return "kind="+kind+" pipeFields="+Math.min(65,pipes+1)
                +" trailingEmpty="+Math.min(65,emptyTail)
                +" semicolons="+Math.min(65,semicolons)+" tabs="+Math.min(65,tabs)+" lineBreaks="+Math.min(65,lines);
    }
}
