package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExprReplace extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprReplace.class, String.class, ExpressionType.SIMPLE,
			"replace [(all|every)|first:[the] first] %strings% in %strings% with %string% [case:with case sensitivity]",
			"replace [(all|every)|first:[the] first] %strings% with %string% in %strings% [case:with case sensitivity]",
			"(replace [with|using] regex|regex replace) %strings% in %strings% with %string%",
			"(replace [with|using] regex|regex replace) %strings% with %string% in %strings%");
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	public boolean isSingle() {
		return false;
	}
	public boolean canBeSingle() {
		return true;
	}

	private Expression<String> exprNeedle;
	private Expression<String> exprHaystack;
	private Expression<String> exprReplacement;

	private boolean isRegex;
	private boolean isFirst;
	private boolean isCaseSensitive = false;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expr, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {

		if (matchedPattern == 0 || matchedPattern == 2) {
			exprHaystack = (Expression<String>) expr[1];
			exprReplacement = (Expression<String>) expr[2];
		} else {
			exprHaystack = (Expression<String>) expr[2];
			exprReplacement = (Expression<String>) expr[1];
		}

		exprNeedle = (Expression<String>) expr[0];

		isRegex = matchedPattern == 2 || matchedPattern == 3;
		isFirst = parseResult.hasTag("first");

		if (SkriptConfig.caseSensitive.value() || parseResult.hasTag("case")) {
			isCaseSensitive = true;
		}
		return true;
	}

	@Override
	@Nullable
	protected String[] get(Event event) {
		String replacement = exprReplacement.getSingle(event);
		String[] needles = exprNeedle.getAll(event);
		String[] haystacks = exprHaystack.getAll(event);

		if (replacement == null) {
			return haystacks;
		}

		List<String> result = new ArrayList<>(haystacks.length);

		if (isRegex) {
			List<Pattern> patterns = new ArrayList<>(needles.length);
			for (String needle : needles) {
				try { // Pre compile regex for use with multiple haystacks
					patterns.add(Pattern.compile(needle));
				} catch (Exception ignored) {
				}
			}

			for (String haystack : haystacks) {
				for (Pattern pattern : patterns) {
					Matcher matcher = pattern.matcher(haystack);
					if (isFirst) {
						haystack = matcher.replaceFirst(replacement);
					} else {
						haystack = matcher.replaceAll(replacement);
					}
				}
				result.add(haystack);
			}
		} else {
			for (String haystack : haystacks) {
				for (String needle : needles) {
					if (isFirst) {
						haystack = StringUtils.replaceFirst(haystack, needle, replacement, isCaseSensitive);
					} else {
						haystack = StringUtils.replace(haystack, needle, replacement, isCaseSensitive);
					}
				}
				result.add(haystack);
			}
		}

		return result.toArray(new String[0]);

	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);

		builder.append("replace");

		if (isFirst) {
			builder.append("first");
		}
		if (isRegex) {
			builder.append("regex");
		}

		builder.append(exprNeedle, "in", exprHaystack, "with", exprReplacement);

		if (isCaseSensitive) {
			builder.append("with case sensitivity");
		}

		return builder.toString();
	}
}
