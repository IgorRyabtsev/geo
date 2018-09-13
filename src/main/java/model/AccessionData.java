package model;

public class AccessionData {
    private String accession;
    private String organism;
    private String genomeAssembly;
    private String donorSex;
    private String donorAge;
    private String downloadURL;

    public AccessionData() {
    }

    public AccessionData(String accession, String organism, String genomeAssembly, String donorSex, String donorAge, String downloadURL) {
        this.accession = accession;
        this.organism = organism;
        this.genomeAssembly = genomeAssembly;
        this.donorSex = donorSex;
        this.donorAge = donorAge;
        this.downloadURL = downloadURL;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getGenomeAssembly() {
        return genomeAssembly;
    }

    public void setGenomeAssembly(String genomeAssembly) {
        this.genomeAssembly = genomeAssembly;
    }

    public String getDonorSex() {
        return donorSex;
    }

    public void setDonorSex(String donorSex) {
        this.donorSex = donorSex;
    }

    public String getDonorAge() {
        return donorAge;
    }

    public void setDonorAge(String donorAge) {
        this.donorAge = donorAge;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    @Override
    public String toString() {
        return "AccessionData{" +
                "accession='" + accession + '\'' +
                ", organism='" + organism + '\'' +
                ", genomeAssembly='" + genomeAssembly + '\'' +
                ", donorSex='" + donorSex + '\'' +
                ", donorAge='" + donorAge + '\'' +
                ", downloadURL='" + downloadURL + '\'' +
                '}';
    }
}
