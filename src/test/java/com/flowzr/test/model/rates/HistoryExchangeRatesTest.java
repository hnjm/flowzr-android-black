/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.test.model.rates;

import com.flowzr.test.db.AbstractDbTest;
import com.flowzr.model.Currency;
import com.flowzr.rates.ExchangeRate;
import com.flowzr.rates.ExchangeRateProvider;
import com.flowzr.test.test.CurrencyBuilder;
import com.flowzr.test.test.DateTime;
import com.flowzr.test.test.RateBuilder;


/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/30/12 7:52 PM
 */
public class HistoryExchangeRatesTest extends AbstractDbTest {

    Currency c1;
    Currency c2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        c1 = CurrencyBuilder.withDb(db).name("USD").title("Dollar").symbol("$").create();
        c2 = CurrencyBuilder.withDb(db).name("EUR").title("Euro").symbol("€").create();
    }

    public void test_should_get_rates_for_every_date() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 7)).rate(0.78592f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78654f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 20)).rate(0.78712f).create();

        ExchangeRateProvider rates = db.getHistoryRates();

        ExchangeRate rate = rates.getRate(c1, c2);
        AssertExchangeRate.assertRate(DateTime.date(2012, 1, 20), 0.78712f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 7).atMidnight().asLong());
        AssertExchangeRate.assertRate(DateTime.date(2012, 1, 7), 0.78592f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 7).at(15, 30, 25, 0).asLong());
        AssertExchangeRate.assertRate(DateTime.date(2012, 1, 7), 0.78592f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 18).at(0, 10, 25, 0).asLong());
        AssertExchangeRate.assertRate(DateTime.date(2012, 1, 18), 0.78654f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 19).at(23, 59, 59, 0).asLong());
        AssertExchangeRate.assertRate(DateTime.date(2012, 1, 18), 0.78654f, rate);

        rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 21).at(23, 59, 59, 0).asLong());
        AssertExchangeRate.assertRate(DateTime.date(2012, 1, 20), 0.78712f, rate);

        rate = rates.getRate(c2, c1, DateTime.date(2012, 1, 21).at(23, 59, 59, 0).asLong());
        AssertExchangeRate.assertRate(DateTime.date(2012, 1, 20), 1.0f / 0.78712f, rate);
    }

    public void test_should_return_error_non_existing_dates() {
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 18)).rate(0.78654f).create();
        RateBuilder.withDb(db).from(c1).to(c2).at(DateTime.date(2012, 1, 19)).rate(0.78712f).create();

        ExchangeRateProvider rates = db.getHistoryRates();
        ExchangeRate rate = rates.getRate(c1, c2, DateTime.date(2012, 1, 7).atMidnight().asLong());
        assertTrue(ExchangeRate.NA == rate);

        // default rate should be cached
        ExchangeRate rate2 = rates.getRate(c1, c2, DateTime.date(1979, 8, 2).atMidnight().asLong());
        assertTrue(ExchangeRate.NA == rate2);
    }

}
