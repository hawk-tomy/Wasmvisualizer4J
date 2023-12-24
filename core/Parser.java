package core;

import core.parser.LEB128Parser;
import core.util.InvalidIndexException;
import core.util.ParseException;
import core.util.Result.Err;
import core.util.Result.Ok;
import core.util.Result.Result;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * note: u** fields are only allow unsigned integer.
 */
public class Parser {

    private final byte[] wasm;
    private final LEB128Parser integerP;
    private Integer uIndex;

    public Parser(byte[] wasm) {
        this.wasm = wasm;
        this.uIndex = 0;
        this.integerP = new LEB128Parser(this);
    }

    public Integer getIndex() {
        return this.uIndex;
    }

    public boolean hasNext() {
        return this.wasm.length > this.uIndex + 1;
    }

    public Result<Byte, InvalidIndexException> peek() {
        return this.peek(0);
    }

    public Result<Byte, InvalidIndexException> peek(int uIdx) {
        int idx = this.uIndex + uIdx;
        if (this.wasm.length <= idx) {
            return new Err<>(new InvalidIndexException());
        }
        return new Ok<>(this.wasm[idx]);
    }

    public Result<Byte, InvalidIndexException> next() {
        if (this.wasm.length <= this.uIndex) {
            return new Err<>(new InvalidIndexException());
        }
        byte b = this.wasm[this.uIndex];
        this.uIndex++;
        return new Ok<>(b);
    }

    public Byte takeByte(byte b, int start) {
        return this.takeByte(b, start, 1);
    }

    /**
     * e.g. this.take((byte)0x01, 0) -> (byte)0x01 this.take((byte)0x10, 5) -> (byte)0x01
     * this.take((byte)0b00000110, 1, 2) -> (byte)0b11 start: 0 <= && < 8 count: 1 <= && <= 8 - start
     * return the Byte.
     */
    public Byte takeByte(byte b, int start, int count) {
        if (start < 0 || 8 <= start || count < 0 || 8 < (start + count)) {
            throw new Error("Invalid argument");
        }
        return (byte) ((b >> start) & ((1 << count) - 1));
    }

    public Result<Void, ParseException> nextByte(byte id) {
        return switch (this.peek()) {
            case Err(InvalidIndexException i) -> new Err<>(new ParseException("Invalid Type ID(expect FunctionType)"));
            case Ok(Byte b) -> {
                if (b == id) {
                    this.next();
                    yield new Ok<>(null);
                } else {
                    yield new Err<>(new ParseException("Invalid Type ID(expect=" + id + ",but got=" + b + ")"));
                }
            }
        };
    }

    public Result<Integer, ParseException> nextU32() {
        return this.integerP.nextU32();
    }

    public Result<Integer, ParseException> nextI32() {
        return this.integerP.nextI32();
    }

    public Result<Long, ParseException> nextU64() {
        return this.integerP.nextU64();
    }

    public Result<Long, ParseException> nextI64() {
        return this.integerP.nextI64();
    }

    public <T> Result<ArrayList<T>, ParseException> nextVector(Function<Parser, Result<T, ParseException>> parse) {
        int uCount;
        switch (this.nextU32()) {
            case Err(ParseException e) -> {return new Err<>(e);}
            case Ok(Integer i) -> uCount = i;
        }
        ArrayList<T> rst = new ArrayList<>();
        for (int i = 0; Integer.compareUnsigned(i, uCount) < 0; i++) {
            switch (parse.apply(this)) {
                case Err(ParseException e) -> {return new Err<>(e);}
                case Ok(T v) -> rst.add(v);
            }
        }
        return new Ok<>(rst);
    }

    public Result<String, ParseException> nextName() {
        // b*: Vector<byte> -> name: if utf(name) == b*
        ArrayList<Byte> bytes;
        // get string bytes. parse: get byte and error convert to correct type.
        switch (this.nextVector((p) -> p
            .next()
            .orElse((e) -> new Err<>(e.into())))) {
            case Err(ParseException e) -> {return new Err<>(e);}
            case Ok(ArrayList<Byte> bytes_) -> bytes = bytes_;
        }
        ByteBuffer buff = ByteBuffer.allocateDirect(bytes.size());
        for (Byte b : bytes) {
            buff.put(b);
        }
        CharsetDecoder d = StandardCharsets.UTF_8.newDecoder();
        try {
            return new Ok<>(d
                .decode(buff)
                .toString());
        } catch (CharacterCodingException e) {
            return new Err<>(new ParseException("utf-8 decode Error:\n" + e));
        }
    }

    public <T> ArrayList<T> parseSequence(Function<Parser, Result<T, ParseException>> parse) {
        ArrayList<T> rets = new ArrayList<>();
        while (true) {
            switch (parse.apply(this)) {
                case Err(ParseException ignored) -> {return rets;}
                case Ok(T ret) -> rets.add(ret);
            }
        }
    }
}
