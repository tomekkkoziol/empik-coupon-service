package com.empik.coupon.application.facade;

import com.empik.coupon.application.command.CreateCouponCommand;
import com.empik.coupon.application.command.CreateCouponCommandHandler;
import com.empik.coupon.application.command.UseCouponCommand;
import com.empik.coupon.application.command.UseCouponCommandHandler;
import com.empik.coupon.application.query.CouponView;
import com.empik.coupon.application.query.GetCouponQuery;
import com.empik.coupon.application.query.GetCouponQueryHandler;
import com.empik.coupon.domain.model.CouponId;
import org.springframework.stereotype.Service;

@Service
public class CouponFacade {

    private final CreateCouponCommandHandler createCouponCommandHandler;
    private final UseCouponCommandHandler useCouponCommandHandler;
    private final GetCouponQueryHandler getCouponQueryHandler;

    public CouponFacade(
            CreateCouponCommandHandler createCouponCommandHandler,
            UseCouponCommandHandler useCouponCommandHandler,
            GetCouponQueryHandler getCouponQueryHandler) {
        this.createCouponCommandHandler = createCouponCommandHandler;
        this.useCouponCommandHandler = useCouponCommandHandler;
        this.getCouponQueryHandler = getCouponQueryHandler;
    }

    public CouponId createCoupon(CreateCouponCommand command) {
        return createCouponCommandHandler.handle(command);
    }

    public void useCoupon(UseCouponCommand command) {
        useCouponCommandHandler.handle(command);
    }

    public CouponView getCoupon(GetCouponQuery query) {
        return getCouponQueryHandler.handle(query);
    }
}
