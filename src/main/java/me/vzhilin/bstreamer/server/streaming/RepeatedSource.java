package me.vzhilin.bstreamer.server.streaming;

import me.vzhilin.bstreamer.server.streaming.base.PullSource;
import me.vzhilin.bstreamer.server.streaming.file.MediaPacket;
import me.vzhilin.bstreamer.server.streaming.file.SourceDescription;

import java.io.IOException;
import java.util.function.Supplier;

public class RepeatedSource implements PullSource {
  private final Supplier<PullSource> supplier;
  private PullSource delegate = null;
  private long lastDts;
  private long lastPts;
  private long ptsOffset = 0;
  private long dtsOffset = 0;

  public RepeatedSource(Supplier<PullSource> supplier) {
    this.supplier = supplier;
  }

  private PullSource ensureHasDelegate() {
    if (delegate == null) {
      delegate = supplier.get();
    }
    return delegate;
  }
  @Override
  public SourceDescription getDesc() {
    ensureHasDelegate();
    return delegate.getDesc();
  }

  @Override
  public boolean hasNext() {
    ensureHasDelegate();

    if (!delegate.hasNext()) {
      ptsOffset += lastPts;
      dtsOffset += lastDts;
      try {
        delegate.close();
      } catch (IOException e) {
        return false;
      }
      delegate = supplier.get();
    }

    return delegate.hasNext();
  }

  @Override
  public MediaPacket next() {
    MediaPacket p = delegate.next();
    lastDts = p.getDts();
    lastPts = p.getPts();
    return new MediaPacket(p.getPts() + ptsOffset, p.getDts() + dtsOffset, p.isKey(), p.getPayload());
  }

  @Override
  public void close() throws IOException {
    if (delegate != null) {
      delegate.close();
      delegate = null;
    }
  }
}
