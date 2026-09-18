package com.nihility.service;

import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.nihility.service.XMPushServiceListener.ConnectionStatus;
import com.xiaomi.push.service.XMPushService;

/**
 * 把 SDK 自带、却从未被启动的「连接超时」看门狗接回去。
 *
 * <p>{@link XMPushService#setConnectingTimeout()} 是 SDK 设计好的机制：进入 connecting 后 15 秒
 * 仍未完成握手，就 {@code disconnect(ERROR_CONNECTIING_TIMEOUT, null)}，把状态机放回 disconnected，
 * 并通过 reason 18 触发接入点惩罚（换 IP），随后由 reconnectionFailed 重新排一次连接。
 * {@code Connection.setConnectionStatus} 在转为 connected／disconnected 时会 {@code removeJobs(10)} 取消它，
 * 说明这是一个完整闭环——但整个 SDK 里没有任何一处调用 setConnectingTimeout()，看门狗从未被装上。
 *
 * <p>后果是 connecting 可以无限期停留：{@code XMPushService.connect()} 以
 * 「try to connect while connecting.」丢弃后续全部重连，{@code networkChanged()} 也因为
 * {@code !isConnecting()} 不再触发重连，直到 TCP 层自己报错。真机日志里出现过 1 小时 52 分的僵死。
 *
 * <p>连接状态进入 connecting 的位置只有一处（{@code SocketConnection.connect()} 调
 * {@code setConnectionStatus(0, 0, null)}），所以在这里按状态变化重新武装看门狗即可覆盖每一次连接尝试。
 */
public class ConnectingTimeoutAbility implements XMPushServiceListener {
    private static final Logger logger = XLog.tag(ConnectingTimeoutAbility.class.getSimpleName()).build();

    private final XMPushService pushService;

    public ConnectingTimeoutAbility(XMPushService pushService) {
        this.pushService = pushService;
    }

    @Override
    public void connectionStatusChanged(ConnectionStatus connectionStatus) {
        if (connectionStatus != ConnectionStatus.connecting) {
            return;
        }

        try {
            logger.d("arm the connecting timeout watchdog");
            pushService.setConnectingTimeout();
        } catch (Throwable e) {
            // 本回调挂在 Connection.setConnectionStatus 的切面上，抛出去会破坏 SDK 自己的状态更新。
            logger.e("cannot arm the connecting timeout watchdog", e);
        }
    }
}
