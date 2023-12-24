package core.AST.Section;

import core.Parser;
import core.util.InvalidIndexException;
import core.util.ParseException;
import core.util.Result.Err;
import core.util.Result.Ok;
import core.util.Result.Result;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.stream.Collectors;

public final class CustomSection implements BaseSection {
    String name;
    ArrayList<Byte> custom;

    CustomSection(String name, ArrayList<Byte> custom) {
        this.name = name;
        this.custom = custom;
    }

    public static Result<CustomSection, ParseException> parse(int length, Parser parser) {
        // load name
        String name;
        int beforeIndex = parser.getIndex();
        switch (parser.nextName()) {
            case Err(ParseException e) -> {return new Err<>(e);}
            case Ok(String name_) -> name = name_;
        }
        int nameLength = parser.getIndex() - beforeIndex;
        int customLength = length - nameLength;
        ArrayList<Byte> custom = new ArrayList<>(customLength);
        for (int i = 0; i < customLength; i++) {
            switch (parser.next()) {
                case Err(InvalidIndexException e) -> {return new Err<>(e.into());}
                case Ok(Byte b) -> custom.add(b);
            }
        }
        return new Ok<>(new CustomSection(name, custom));
    }

    public String toString() {
        HexFormat f = HexFormat.of();
        String s = this.custom
            .subList(0, 10)
            .stream()
            .map(f::toHexDigits)
            .collect(Collectors.joining());
        return (
            "CustomSection(\n"
            + (
                "name='" + this.name + "'\n"
                + "custom=" + s + "..."
            ).indent(2)
            + "\n)"
        );

    }
}
