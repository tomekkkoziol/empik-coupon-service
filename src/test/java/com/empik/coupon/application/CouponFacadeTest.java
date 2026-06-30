package com.empik.coupon.application;

import com.empik.coupon.application.command.CreateCouponCommand;
import com.empik.coupon.application.command.CreateCouponCommandHandler;
import com.empik.coupon.application.command.UseCouponCommand;
import com.empik.coupon.application.command.UseCouponCommandHandler;
import com.empik.coupon.application.facade.CouponFacade;
import com.empik.coupon.application.query.CouponView;
import com.empik.coupon.application.query.GetCouponQuery;
import com.empik.coupon.application.query.GetCouponQueryHandler;
import com.empik.coupon.domain.model.CouponId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CouponFacadeTest {

    @Mock
    CreateCouponCommandHandler createHandler;

    @Mock
    UseCouponCommandHandler useHandler;

    @Mock
    GetCouponQueryHandler getHandler;

    CouponFacade facade;

    @BeforeEach
    void setUp() {
        facade = new CouponFacade(createHandler, useHandler, getHandler);
    }

    @Test
    void should_delegate_create_to_command_handler() {
        CreateCouponCommand command = new CreateCouponCommand("WIOSNA", 10, "PL");
        CouponId expectedId = CouponId.generate();
        given(createHandler.handle(command)).willReturn(expectedId);

        CouponId result = facade.createCoupon(command);

        assertThat(result).isEqualTo(expectedId);
        then(createHandler).should().handle(command);
    }

    @Test
    void should_delegate_use_to_command_handler() {
        UseCouponCommand command = new UseCouponCommand("WIOSNA", "user-1", "5.10.10.10");

        facade.useCoupon(command);

        then(useHandler).should().handle(command);
    }

    @Test
    void should_delegate_get_to_query_handler() {
        GetCouponQuery query = new GetCouponQuery("WIOSNA");
        CouponView expectedView = new CouponView(
                UUID.randomUUID().toString(), "WIOSNA", Instant.now(), 10, 0, "PL", false);
        given(getHandler.handle(query)).willReturn(expectedView);

        CouponView result = facade.getCoupon(query);

        assertThat(result).isEqualTo(expectedView);
        then(getHandler).should().handle(query);
    }
}
