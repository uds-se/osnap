package de.unisaarland.cs.st.util;

import java.util.ListIterator;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.ParseException;

public class IgnoringUnderclaredOptionsParser extends BasicParser {

	public IgnoringUnderclaredOptionsParser(boolean ignoreUnrecognizedOption) {
		super();
		this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
	}

	public IgnoringUnderclaredOptionsParser() {
		super();
		this.ignoreUnrecognizedOption = true;
	}

	private boolean ignoreUnrecognizedOption;

	@Override
	protected void processOption(String arg, ListIterator iter)
			throws ParseException {
		boolean hasOption = getOptions().hasOption(arg);

		if (hasOption || !ignoreUnrecognizedOption) {
			super.processOption(arg, iter);
		}
	}
}
