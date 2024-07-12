select *
  from customer
  where c_custkey = (select max(o_custkey)
                        from orders
                        where subdate(o_orderdate, interval '1' DAY) < '2022-12-20')
