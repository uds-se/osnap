package de.unisaarland.cs.st.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Package implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6475333381586413491L;
    // By default we consider only Package Name, not version
    private static final boolean STRICT = Boolean.parseBoolean(System.getProperty("package.strict.equality", "false"));

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((conflicts == null) ? 0 : conflicts.hashCode());
	result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
	result = prime * result + downloadTime;
	result = prime * result + installationTime;
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	result = prime * result + ((version == null) ? 0 : version.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Package other = (Package) obj;

	if (STRICT) {

	    if (conflicts == null) {
		if (other.conflicts != null)
		    return false;
	    } else if (!conflicts.equals(other.conflicts))
		return false;
	    if (dependencies == null) {
		if (other.dependencies != null)
		    return false;
	    } else if (!dependencies.equals(other.dependencies))
		return false;
	    if (downloadTime != other.downloadTime)
		return false;
	    if (installationTime != other.installationTime)
		return false;
	    if (name == null) {
		if (other.name != null)
		    return false;
	    } else if (!name.equals(other.name))
		return false;
	    if (version == null) {
		if (other.version != null)
		    return false;
	    } else if (!version.equals(other.version))
		return false;
	} else {
	    if (name == null) {
		if (other.name != null)
		    return false;
	    } else if (!name.equals(other.name))
		return false;
	}
	return true;
    }

    public String name;
    public String version;
    // Time to download locally this package - millisec
    public int downloadTime;
    public int installationTime;
    // public int sourceDownloadTime;
    // public int sourceInstallationTime;

    public Set<Package> dependencies = new HashSet<Package>();
    public Set<Package> conflicts = new HashSet<Package>();

    @Override
    public String toString() {
	return String.format("%s%s", name, (version != null) ? "_" + version : "");
    }
}
