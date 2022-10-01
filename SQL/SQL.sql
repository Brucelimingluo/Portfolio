-- Before running drop any existing views
DROP VIEW IF EXISTS q0;
DROP VIEW IF EXISTS q1i;
DROP VIEW IF EXISTS q1ii;
DROP VIEW IF EXISTS q1iii;
DROP VIEW IF EXISTS q1iv;
DROP VIEW IF EXISTS q2i;
DROP VIEW IF EXISTS q2ii;
DROP VIEW IF EXISTS q2iii;
DROP VIEW IF EXISTS q3i;
DROP VIEW IF EXISTS q3ii;
DROP VIEW IF EXISTS q3iii;
DROP VIEW IF EXISTS q4i;
DROP VIEW IF EXISTS q4ii;
DROP VIEW IF EXISTS q4iii;
DROP VIEW IF EXISTS q4iv;
DROP VIEW IF EXISTS q4v;


CREATE VIEW q0(era)
AS
  SELECT MAX(era)
  FROM pitching
;


CREATE VIEW q1i(namefirst, namelast, birthyear)
AS
  SELECT namefirst, namelast, birthyear
  from people
  where weight > 300
;


CREATE VIEW q1ii(namefirst, namelast, birthyear)
AS
  SELECT namefirst, namelast, birthyear
  from people
  where nameFirst like'% %'
  order by namefirst, namelast

;


CREATE VIEW q1iii(birthyear, avgheight, count)
AS
  SELECT birthyear, avg(height), COUNT(*)
  from people
  group by birthyear
;


CREATE VIEW q1iv(birthyear, avgheight, count)
AS
  SELECT birthyear, avg(height), COUNT(*)
  from people
  group by birthyear
  having avg(height) > 70
;

CREATE VIEW q2i(namefirst, namelast, playerid, yearid)
AS

  select namefirst, namelast, h.playerid, yearid
  from halloffame h, people p
  where h.playerid = p.playerid and inducted = 'Y'
  order by yearid desc, h.playerid
;

CREATE VIEW q2ii(namefirst, namelast, playerid, schoolid, yearid)
AS
  select namefirst, namelast, c.playerid, c.schoolid, h.yearid
  from people p, HallOfFame h, CollegePlaying c, schools s
  where h.playerid = p.playerid and h.playerID = c.playerid
        and c.schoolid = s.schoolid
        and s.schoolState = 'CA' and inducted = 'Y'
  order by h.yearid desc, c.schoolid, c.playerID
;

CREATE VIEW q2iii(playerid, namefirst, namelast, schoolid)
AS
  select h.playerid, namefirst, namelast, c.schoolID
  from people p, HallOfFame h left join CollegePlaying c on h.playerid = c.playerid
  where inducted = 'Y' and h.playerid = p.playerid
  order by h.playerid desc, c.schoolid
;

CREATE VIEW q3i(playerid, namefirst, namelast, yearid, slg)
AS
  select p.playerid, namefirst, namelast, yearid, cast((H+H2B+2*H3B+3*HR) as float) /(AB) as slg
  from batting b, people p
  where p.playerid = b.playerID and AB > 50
  order by slg desc, yearid, p.playerid
  limit 10
;

CREATE VIEW q3ii(playerid, namefirst, namelast, lslg)
AS
  select p.playerid, namefirst, namelast, cast((sum(H)+sum(H2B)+sum(2*H3B)+sum(3*HR)) as float) /(sum(AB)) as lslg
  from batting b, people p
  where p.playerid = b.playerID
  group by p.playerid
  having sum(AB) > 50
  order by lslg desc, yearid, p.playerid
  limit 10
;

CREATE VIEW q3iii(namefirst, namelast, lslg)
AS
  select namefirst, namelast, cast((sum(H)+sum(H2B)+sum(2*H3B)+sum(3*HR)) as float) /(sum(AB)) as lslg
  from batting b, people p
  where p.playerid = b.playerID
  group by p.playerID
  having sum(AB) > 50
      and lslg > (select cast((sum(H)+sum(H2B)+sum(2*H3B)+sum(3*HR)) as float) /(sum(AB))
                from batting b
                where b.playerid = 'mayswi01')
  order by lslg desc, p.playerID
;

CREATE VIEW q4i(yearid, min, max, avg)
AS
  select yearid, min(salary), max(salary), avg(salary)
  from salaries
  group by yearid
  order by yearid
;

CREATE VIEW q4ii(binid, low, high, count)
AS
  with lowest as (select min(salary) min from salaries where yearid = 2016 ),
       largest as (select max(salary) max from salaries where yearid = 2016 )
  select binid, (select lowest.min + ((largest.max-lowest.min)/10)*binid) low,
                (select lowest.min + ((largest.max-lowest.min)/10)*(1+binid)) high,
                        count(*)
  from lowest, largest, binids, salaries
  where salaries.yearid = '2016' and salary >= low
      and (((binid = 9) and (salary < high)) or (salary <= high))
  group by binid
  order by binid
;

CREATE VIEW q4iii(yearid, mindiff, maxdiff, avgdiff)
AS
  select yearid, min_diff, max_diff, avg_diff from
    (select yearid,
            min_s-lag(min_s, 1) over (order by yearid) min_diff,
            max_s-lag(max_s, 1) over (order by yearid) max_diff,
            avg_s-lag(avg_s, 1) over (order by yearid) avg_diff
    from (select yearid, min(salary) min_s, max(salary) max_s, avg(salary) avg_s
          from salaries
          group by yearid
          order by yearid))
  where min_diff is not null
    and max_diff is not null
    and avg_diff is not null
;

CREATE VIEW q4iv(playerid, namefirst, namelast, salary, yearid)
AS
  select p.playerid, namefirst, namelast, salary, yearid
  from people p, salaries s
  where p.playerid = s.playerid and (s.yearid = 2000 and s.salary = (select max(salary) from salaries where yearid = 2000)
    or (s.yearid = 2001 and s.salary = (select max(salary) from salaries where yearid = 2001)))
;

CREATE VIEW q4v(team, diffAvg) AS
  select a.teamid, max(s.salary)-min(s.salary)
  from allstarfull a, salaries s
  where a.playerid = s.playerid and a.yearid = 2016 and s.yearid = 2016
  group by a.teamid
  order by a.teamid
;
